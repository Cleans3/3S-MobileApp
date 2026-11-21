package vn.shelftap.app.data

import java.io.IOException

/** Data plus whether it came from the local cache because the network was unavailable. */
data class Snapshot<T>(val data: T, val fromCache: Boolean)

class ShelfRepository(
    private val api: ShelfTapApi,
    private val cache: LocalCache,
) {
    suspend fun loans(): Snapshot<List<Loan>> = withFallback(
        cached = { cache.loans() },
        save = { cache.saveLoans(it) },
        fetch = { api.loans() },
    )

    suspend fun history(): Snapshot<List<HistoryItem>> = withFallback(
        cached = { cache.history() },
        save = { cache.saveHistory(it) },
        fetch = { api.history() },
    )

    suspend fun me(): Snapshot<Me> = withFallback(
        cached = { cache.me() },
        save = { cache.saveMe(it) },
        fetch = { api.me() },
    )

    suspend fun setHistoryEnabled(enabled: Boolean): Me {
        val updated = api.setHistoryEnabled(enabled)
        cache.saveMe(updated)
        return updated
    }

    suspend fun renew(loanId: String): Loan {
        val updated = api.renew(loanId)
        cache.loans()?.let { current ->
            cache.saveLoans(current.map { if (it.loanId == updated.loanId) updated else it })
        }
        return updated
    }

    suspend fun title(titleId: String): Title = api.title(titleId)

    /** Fetches from the network and caches the result; on a network failure falls back to the cache. */
    private suspend fun <T> withFallback(
        cached: () -> T?,
        save: (T) -> Unit,
        fetch: suspend () -> T,
    ): Snapshot<T> = try {
        val fresh = fetch()
        save(fresh)
        Snapshot(fresh, fromCache = false)
    } catch (e: IOException) {
        val saved = cached() ?: throw e
        Snapshot(saved, fromCache = true)
    }
}
