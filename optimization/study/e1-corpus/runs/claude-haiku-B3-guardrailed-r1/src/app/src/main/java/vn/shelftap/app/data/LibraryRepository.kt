package vn.shelftap.app.data

import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import vn.shelftap.app.api.HistoryItem
import vn.shelftap.app.api.Loan
import vn.shelftap.app.api.Me
import vn.shelftap.app.api.ShelfTapApi
import vn.shelftap.app.api.TitleDetail

/** Data plus whether it came from the encrypted cache because the network was unavailable. */
class Snapshot<T>(val data: T, val fromCache: Boolean)

class LibraryRepository(
    private val api: ShelfTapApi,
    private val cache: SecureCache,
    private val io: CoroutineDispatcher,
) {

    suspend fun loans(): Snapshot<List<Loan>> =
        fetchOrCache(KEY_LOANS, ListSerializer(Loan.serializer())) { api.loans() }

    /** Newest first. */
    suspend fun history(): Snapshot<List<HistoryItem>> =
        fetchOrCache(KEY_HISTORY, ListSerializer(HistoryItem.serializer())) {
            api.history().sortedByDescending { Instant.parse(it.outAt) }
        }

    suspend fun me(): Snapshot<Me> =
        fetchOrCache(KEY_ME, Me.serializer()) { api.me() }

    suspend fun renew(loanId: String): Loan = withContext(io) {
        val updated = api.renew(loanId)
        val serializer = ListSerializer(Loan.serializer())
        cache.load(KEY_LOANS, serializer)?.let { cached ->
            cache.save(KEY_LOANS, serializer, cached.map { if (it.loanId == loanId) updated else it })
        }
        updated
    }

    suspend fun setHistoryEnabled(enabled: Boolean): Me = withContext(io) {
        val me = api.updatePrivacy(enabled)
        cache.save(KEY_ME, Me.serializer(), me)
        me
    }

    suspend fun title(titleId: String): TitleDetail = withContext(io) {
        api.title(titleId)
    }

    /**
     * Fetches from the network and refreshes the encrypted cache. When the network is
     * unavailable, returns the last saved data; throws only if nothing was saved.
     */
    private suspend fun <T> fetchOrCache(
        key: String,
        serializer: KSerializer<T>,
        fetch: suspend () -> T,
    ): Snapshot<T> = withContext(io) {
        try {
            val fresh = fetch()
            cache.save(key, serializer, fresh)
            Snapshot(fresh, fromCache = false)
        } catch (e: IOException) {
            val saved = cache.load(key, serializer) ?: throw e
            Snapshot(saved, fromCache = true)
        }
    }

    private companion object {
        const val KEY_LOANS = "loans"
        const val KEY_HISTORY = "history"
        const val KEY_ME = "me"
    }
}
