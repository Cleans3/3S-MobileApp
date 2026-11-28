package shelftap.android.recommend

import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import shelftap.android.api.ApiException
import shelftap.android.api.ErrorCode
import shelftap.android.api.HistoryItem
import shelftap.android.api.Loan
import shelftap.android.api.Me
import shelftap.android.api.Reservation
import shelftap.android.api.ReservationStatus

/** What the service needs from the server, so it can be tested with a fake. Every call may throw [ApiException]. */
interface RecommendSource {
    suspend fun me(): Me
    suspend fun loans(): List<Loan>
    suspend fun reservations(): List<Reservation>
    suspend fun history(): List<HistoryItem>
    suspend fun book(titleId: String): BookInfo
    suspend fun search(query: String): SearchHits

    /** The size of the whole catalog when some list already reported it (no request), else null. */
    fun knownCatalogSize(): Long?
}

data class SearchHits(val books: List<BookInfo>, val total: Long)

/**
 * Suggestions for the signed-in member, computed on the phone from their loans, reservations and (when it is on and the login allows it)
 * reading history; see [Recommender]. The API has no catalog download yet (plan A2), so candidates come from a few searches: the member's
 * two favourite authors and two favourite subjects. That is at most [MAX_SIGNALS] + 1 + 4 requests on top of the four reads of the
 * member's own data, which stays inside the server's 20 requests a minute; book details are kept for the session.
 *
 * Nothing is stored: the details, the profile and the turned-down books live in memory and are dropped when the session ends ([bind]).
 */
class RecommendationService(
    private val source: RecommendSource,
    /** The member's own switch "Suggestions from my reading history" (on by default). */
    private val useHistory: Flow<Boolean>,
    private val now: () -> Instant = Instant::now,
) {
    /** What the suggestions are built from: the whole picture, loans only because the card login may not read the history, or loans only because the history is off. */
    enum class Basis { FULL, CARD, NO_HISTORY }

    sealed interface State {
        data object Idle : State
        data object Loading : State
        data class Ready(val items: List<Recommendation>, val basis: Basis) : State

        /** Nothing to suggest: nothing borrowed yet, or the history is off and nothing is on loan. */
        data class Empty(val basis: Basis) : State
        data class Failed(val error: ApiException) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val lock = Mutex()
    private val details = HashMap<String, BookInfo>()
    private val dismissed = HashSet<String>()
    private var signals: List<Signal> = emptyList()
    private var excluded: Set<String> = emptySet()
    private var pool: List<BookInfo> = emptyList()
    private val df = HashMap<Feature, Long>()
    private var catalogSize = 0L
    private var basis = Basis.FULL

    /** Computes the suggestions unless they are there already (or being computed); a failed attempt is tried again. */
    suspend fun ensureLoaded() {
        val s = _state.value
        if (s is State.Idle || s is State.Failed) load()
    }

    suspend fun refresh() = load()

    /** "Not interested": the book leaves the list now and stays out until the session ends. */
    fun dismiss(titleId: String) {
        dismissed += titleId
        if (_state.value is State.Ready || _state.value is State.Empty) rescore()
    }

    /** The inputs changed (the history switch): the next time the suggestions are wanted they are worked out again. What was learnt about books and what was turned down stays. */
    fun invalidate() {
        _state.value = State.Idle
    }

    /** Forgets everything: the member signed out or the login was lost. */
    fun clear() {
        details.clear(); dismissed.clear(); pool = emptyList(); signals = emptyList(); excluded = emptySet(); df.clear(); catalogSize = 0
        _state.value = State.Idle
    }

    /** Clears the service whenever the session stops being active. */
    fun bind(scope: CoroutineScope, active: Flow<Boolean>) {
        active.onEach { if (!it) clear() }.launchIn(scope)
    }

    private suspend fun load() {
        lock.withLock { loadLocked() }
    }

    private suspend fun loadLocked() {
        _state.value = State.Loading
        try {
            val me = source.me()
            val loans = source.loans()
            val reservations = source.reservations().filter { it.status == ReservationStatus.WAITING || it.status == ReservationStatus.READY }
            basis = if (me.historyEnabled && useHistory.first()) Basis.FULL else Basis.NO_HISTORY
            val history = if (basis == Basis.FULL) {
                try {
                    source.history()
                } catch (e: ApiException.Http) {
                    // A card login may not read the history: suggest from what is on loan and say so.
                    if (e.code != ErrorCode.STEP_UP_REQUIRED) throw e
                    basis = Basis.CARD
                    emptyList()
                }
            } else {
                emptyList()
            }

            excluded = (loans.map { it.titleId } + reservations.map { it.titleId } + history.map { it.titleId }).toSet()
            val weights = HashMap<String, Double>()
            fun count(id: String, w: Double) { weights[id] = maxOf(weights[id] ?: 0.0, w) }
            loans.filter { it.returnedAt == null }.forEach { count(it.titleId, 1.0) }
            reservations.forEach { count(it.titleId, 1.0) }
            history.forEach { h -> count(h.titleId, h.returnedAt?.let { Recommender.recencyWeight(daysSince(it)) } ?: 1.0) }
            val top = weights.entries.sortedByDescending { it.value }.take(MAX_SIGNALS)

            signals = top.mapNotNull { (id, w) -> details[id]?.let { Signal(it, w) } ?: fetch(id)?.let { Signal(it, w) } }
            if (signals.isEmpty()) {
                pool = emptyList()
                _state.value = State.Empty(basis)
                return
            }

            val taste = Recommender.profile(signals) { 1.0 }
            val favourites = taste.filterKeys { it is Feature.Author }.entries.sortedByDescending { it.value }.take(FAVOURITE_AUTHORS).map { it.key } +
                taste.filterKeys { it is Feature.Subject }.entries.sortedByDescending { it.value }.take(FAVOURITE_SUBJECTS).map { it.key }
            catalogSize = source.knownCatalogSize() ?: source.search("").total
            val found = LinkedHashMap<String, BookInfo>()
            for (f in favourites) {
                val hits = source.search(f.name)
                df[f] = hits.total
                hits.books.forEach { found.putIfAbsent(it.titleId, it) }
            }
            pool = found.values.toList()
            rescore()
        } catch (e: ApiException) {
            _state.value = State.Failed(e)
        }
    }

    /** A book's details, or null when the server no longer has it (it is then left out of the profile). */
    private suspend fun fetch(id: String): BookInfo? =
        try {
            source.book(id).also { details[id] = it }
        } catch (e: ApiException.Http) {
            if (e.code == ErrorCode.NOT_FOUND) null else throw e
        }

    private fun rescore() {
        val size = catalogSize
        val found = Recommender.recommend(signals, pool, excluded + dismissed, idf = { f -> Recommender.idf(size, df[f]) })
        _state.value = if (found.isEmpty()) State.Empty(basis) else State.Ready(found, basis)
    }

    private fun daysSince(iso: String): Double =
        try {
            Duration.between(Instant.parse(iso), now()).toMillis() / MILLIS_PER_DAY
        } catch (_: DateTimeParseException) {
            0.0
        }

    companion object {
        /** The books the profile is built from: the most recent and the ones in hand. */
        const val MAX_SIGNALS = 6
        const val FAVOURITE_AUTHORS = 2
        const val FAVOURITE_SUBJECTS = 2
        private const val MILLIS_PER_DAY = 86_400_000.0
    }
}
