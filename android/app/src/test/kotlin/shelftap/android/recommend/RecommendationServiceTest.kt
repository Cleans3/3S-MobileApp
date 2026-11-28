package shelftap.android.recommend

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import shelftap.android.api.ApiException
import shelftap.android.api.ErrorCode
import shelftap.android.api.HistoryItem
import shelftap.android.api.Loan
import shelftap.android.api.Me
import shelftap.android.api.Reservation
import shelftap.android.api.ReservationStatus

/** What the service reads, in which order and how often, and what it shows for each kind of login. */
class RecommendationServiceTest {
    private val clock = Instant.parse("2026-11-28T00:00:00Z")

    private fun book(id: String, authors: List<String> = emptyList(), subjects: List<String> = emptyList(), copies: Int = 1) =
        BookInfo(id, "Book $id", authors, subjects, copies, null)

    private fun loan(id: String, returnedAt: String? = null) =
        Loan("l$id", "c$id", id, "Book $id", "bc$id", "2026-11-01T00:00:00Z", "2026-11-15T00:00:00Z", returnedAt, 0, true)

    private fun item(id: String, returnedAt: String?) = HistoryItem(id, "Book $id", "2026-10-01T00:00:00Z", returnedAt)

    private class Fake(
        val historyEnabled: Boolean = true,
        val loans: List<Loan> = emptyList(),
        val reservations: List<Reservation> = emptyList(),
        val historyOf: () -> List<HistoryItem> = { emptyList() },
        val books: Map<String, BookInfo> = emptyMap(),
        val catalog: Map<String, SearchHits> = emptyMap(),
        val catalogSize: Long? = 1000,
    ) : RecommendSource {
        val calls = ArrayList<String>()
        override suspend fun me(): Me { calls += "me"; return Me("m", "Member", "m@x", "ADULT", "ACTIVE", historyEnabled, false) }
        override suspend fun loans(): List<Loan> { calls += "loans"; return loans }
        override suspend fun reservations(): List<Reservation> { calls += "reservations"; return reservations }
        override suspend fun history(): List<HistoryItem> { calls += "history"; return historyOf() }
        override suspend fun book(titleId: String): BookInfo {
            calls += "book:$titleId"
            return books[titleId] ?: throw ApiException.Http(404, ErrorCode.NOT_FOUND, null)
        }
        override suspend fun search(query: String): SearchHits { calls += "search:$query"; return catalog[query] ?: SearchHits(emptyList(), 0) }
        override fun knownCatalogSize(): Long? = catalogSize
    }

    private fun service(source: RecommendSource, useHistory: Boolean = true) =
        RecommendationService(source, flowOf(useHistory), now = { clock })

    private val kieu = book("a", listOf("Nam Cao"), listOf("Truyện ngắn"))
    private val more = SearchHits(listOf(book("x", listOf("Nam Cao"), listOf("Truyện ngắn")), book("y", listOf("Other"), listOf("Truyện ngắn"))), 40)

    @Test
    fun aMemberWithABookOnLoanGetsSuggestionsAndTheBookIsNotOneOfThem(): Unit = runBlocking {
        val src = Fake(loans = listOf(loan("a")), books = mapOf("a" to kieu), catalog = mapOf("Nam Cao" to more, "Truyện ngắn" to more))
        val s = service(src)
        s.ensureLoaded()
        val ready = assertIs<RecommendationService.State.Ready>(s.state.value)
        assertEquals(RecommendationService.Basis.FULL, ready.basis)
        assertTrue(ready.items.isNotEmpty())
        assertTrue(ready.items.none { it.book.titleId == "a" })
        assertEquals("x", ready.items.first().book.titleId)
    }

    @Test
    fun aCardLoginSuggestsFromWhatIsOnLoanAndSaysSo(): Unit = runBlocking {
        val src = Fake(
            loans = listOf(loan("a")), books = mapOf("a" to kieu), catalog = mapOf("Nam Cao" to more, "Truyện ngắn" to more),
            historyOf = { throw ApiException.Http(403, ErrorCode.STEP_UP_REQUIRED, null) },
        )
        val s = service(src)
        s.ensureLoaded()
        assertEquals(RecommendationService.Basis.CARD, assertIs<RecommendationService.State.Ready>(s.state.value).basis)
    }

    @Test
    fun withTheHistorySwitchedOffTheHistoryIsNeverRead(): Unit = runBlocking {
        val src = Fake(loans = listOf(loan("a")), books = mapOf("a" to kieu), catalog = mapOf("Nam Cao" to more, "Truyện ngắn" to more))
        service(src, useHistory = false).also { it.ensureLoaded() }
        assertTrue("history" !in src.calls)
        val off = Fake(historyEnabled = false, loans = listOf(loan("a")), books = mapOf("a" to kieu), catalog = mapOf("Nam Cao" to more, "Truyện ngắn" to more))
        val s = service(off)
        s.ensureLoaded()
        assertTrue("history" !in off.calls)
        assertEquals(RecommendationService.Basis.NO_HISTORY, assertIs<RecommendationService.State.Ready>(s.state.value).basis)
    }

    @Test
    fun nothingBorrowedGivesAnEmptyStateAndNoSearch(): Unit = runBlocking {
        val src = Fake()
        val s = service(src)
        s.ensureLoaded()
        assertIs<RecommendationService.State.Empty>(s.state.value)
        assertTrue(src.calls.none { it.startsWith("search") || it.startsWith("book") })
    }

    @Test
    fun aReturnedBookInTheHistoryCountsAndAnOldOneCountsLess(): Unit = runBlocking {
        val recent = book("r", listOf("New Author"), listOf("S1"))
        val old = book("o", listOf("Old Author"), listOf("S2"))
        val src = Fake(
            books = mapOf("r" to recent, "o" to old),
            historyOf = { listOf(item("r", "2026-11-20T00:00:00Z"), item("o", "2025-06-01T00:00:00Z")) },
            catalog = mapOf(
                "New Author" to SearchHits(listOf(book("n", listOf("New Author"))), 5),
                "Old Author" to SearchHits(listOf(book("p", listOf("Old Author"))), 5),
                "S1" to SearchHits(emptyList(), 50), "S2" to SearchHits(emptyList(), 50),
            ),
        )
        val s = service(src)
        s.ensureLoaded()
        val ready = assertIs<RecommendationService.State.Ready>(s.state.value)
        assertEquals(listOf("n", "p"), ready.items.map { it.book.titleId })
    }

    @Test
    fun aBookTheMemberReservedOrReturnedIsNotSuggestedBack(): Unit = runBlocking {
        val res = Reservation("r1", "x", "Book x", "2026-11-20T00:00:00Z", null, ReservationStatus.WAITING)
        val src = Fake(loans = listOf(loan("a")), reservations = listOf(res), books = mapOf("a" to kieu, "x" to more.books[0]), catalog = mapOf("Nam Cao" to more, "Truyện ngắn" to more))
        val s = service(src)
        s.ensureLoaded()
        val ids = assertIs<RecommendationService.State.Ready>(s.state.value).items.map { it.book.titleId }
        assertTrue("x" !in ids && "a" !in ids)
    }

    @Test
    fun turningABookDownRemovesItAtOnceWithoutAskingTheServerAgain(): Unit = runBlocking {
        val src = Fake(loans = listOf(loan("a")), books = mapOf("a" to kieu), catalog = mapOf("Nam Cao" to more, "Truyện ngắn" to more))
        val s = service(src)
        s.ensureLoaded()
        val before = src.calls.size
        s.dismiss("x")
        val ids = (s.state.value as RecommendationService.State.Ready).items.map { it.book.titleId }
        assertTrue("x" !in ids)
        assertEquals(before, src.calls.size)
        s.refresh() // the book stays out for the session
        assertTrue("x" !in (s.state.value as RecommendationService.State.Ready).items.map { it.book.titleId })
    }

    @Test
    fun loadingTwiceDoesNotAskTheServerTwice(): Unit = runBlocking {
        val src = Fake(loans = listOf(loan("a")), books = mapOf("a" to kieu), catalog = mapOf("Nam Cao" to more, "Truyện ngắn" to more))
        val s = service(src)
        s.ensureLoaded()
        val n = src.calls.size
        s.ensureLoaded()
        assertEquals(n, src.calls.size)
    }

    @Test
    fun aFailureIsShownAndTheNextTryAsksAgain(): Unit = runBlocking {
        var down = true
        val src = object : RecommendSource by Fake(loans = listOf(loan("a")), books = mapOf("a" to kieu), catalog = mapOf("Nam Cao" to more, "Truyện ngắn" to more)) {
            override suspend fun me(): Me = if (down) throw ApiException.Network(java.io.IOException("offline")) else Me("m", "M", "m@x", "ADULT", "ACTIVE", true, false)
        }
        val s = service(src)
        s.ensureLoaded()
        assertIs<RecommendationService.State.Failed>(s.state.value)
        down = false
        s.ensureLoaded()
        assertIs<RecommendationService.State.Ready>(s.state.value)
    }

    @Test
    fun aFirstLoadStaysInsideTheServersTwentyRequestsAMinute(): Unit = runBlocking {
        val many = (1..8).associate { "b$it" to book("b$it", listOf("Author $it"), listOf("Subject $it")) }
        val src = Fake(
            loans = (1..8).map { loan("b$it") }, books = many,
            catalog = (1..8).associate { "Author $it" to SearchHits(listOf(book("c$it", listOf("Author $it"))), 3) },
            catalogSize = null,
        )
        service(src).ensureLoaded()
        assertTrue(src.calls.size <= 20, "calls: ${src.calls.size} ${src.calls}")
    }

    @Test
    fun aBookTheServerNoLongerHasIsLeftOutOfTheProfile(): Unit = runBlocking {
        val src = Fake(loans = listOf(loan("gone"), loan("a")), books = mapOf("a" to kieu), catalog = mapOf("Nam Cao" to more, "Truyện ngắn" to more))
        val s = service(src)
        s.ensureLoaded()
        assertIs<RecommendationService.State.Ready>(s.state.value)
    }

    @Test
    fun leavingTheSessionClearsEverything(): Unit = runBlocking {
        val src = Fake(loans = listOf(loan("a")), books = mapOf("a" to kieu), catalog = mapOf("Nam Cao" to more, "Truyện ngắn" to more))
        val s = service(src)
        s.ensureLoaded()
        s.dismiss("x")
        s.clear()
        assertIs<RecommendationService.State.Idle>(s.state.value)
        s.ensureLoaded() // a new session starts from nothing: the turned-down book is back
        assertTrue("x" in (s.state.value as RecommendationService.State.Ready).items.map { it.book.titleId })
    }

    @Test
    fun theServiceFollowsTheSession(): Unit = runBlocking {
        val src = Fake(loans = listOf(loan("a")), books = mapOf("a" to kieu), catalog = mapOf("Nam Cao" to more, "Truyện ngắn" to more))
        val s = service(src)
        val active = MutableStateFlow(true)
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        s.bind(scope, active)
        s.ensureLoaded()
        assertIs<RecommendationService.State.Ready>(s.state.value)
        active.value = false
        assertIs<RecommendationService.State.Idle>(s.state.value)
    }
}
