package vn.shelftap.app.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import java.io.IOException
import vn.shelftap.app.net.ApiClient

/** A value plus whether it came from the encrypted local copy (network unavailable). */
data class Loaded<T>(val value: T, val fromCache: Boolean)

class LibraryRepository(
    private val api: ApiClient,
    private val cache: SecureCache,
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun loans(): Loaded<List<Loan>> =
        withCache(CACHE_LOANS, ListSerializer(Loan.serializer())) { api.loans() }

    suspend fun history(): Loaded<List<HistoryItem>> =
        withCache(CACHE_HISTORY, ListSerializer(HistoryItem.serializer())) { api.history() }

    suspend fun member(): Loaded<Member> =
        withCache(CACHE_MEMBER, Member.serializer()) { api.me() }

    suspend fun renew(loanId: String): Loan = withContext(ioDispatcher) {
        val updated = api.renewLoan(loanId)
        val loansSerializer = ListSerializer(Loan.serializer())
        readCached(CACHE_LOANS, loansSerializer)?.let { cached ->
            writeCached(
                CACHE_LOANS,
                loansSerializer,
                cached.map { if (it.loanId == loanId) updated else it },
            )
        }
        updated
    }

    suspend fun setHistoryEnabled(enabled: Boolean): Member = withContext(ioDispatcher) {
        api.setHistoryEnabled(enabled).also { writeCached(CACHE_MEMBER, Member.serializer(), it) }
    }

    suspend fun title(titleId: String): Title = api.title(titleId)

    /**
     * Fetches fresh data and stores it encrypted. If the network is unavailable, falls back
     * to the last saved copy; if there is none, the original IOException is rethrown.
     * Other failures (HTTP errors) propagate unchanged.
     */
    private suspend fun <T> withCache(
        name: String,
        serializer: KSerializer<T>,
        fetch: suspend () -> T,
    ): Loaded<T> = withContext(ioDispatcher) {
        try {
            val fresh = fetch()
            writeCached(name, serializer, fresh)
            Loaded(fresh, fromCache = false)
        } catch (offline: IOException) {
            val saved = readCached(name, serializer) ?: throw offline
            Loaded(saved, fromCache = true)
        }
    }

    private fun <T> writeCached(name: String, serializer: KSerializer<T>, value: T) {
        // A failed local save must not fail a request that already succeeded.
        runCatching { cache.save(name, ShelfTapJson.encodeToString(serializer, value)) }
    }

    private fun <T> readCached(name: String, serializer: KSerializer<T>): T? =
        runCatching {
            cache.load(name)?.let { ShelfTapJson.decodeFromString(serializer, it) }
        }.getOrNull()

    private companion object {
        const val CACHE_LOANS = "loans.bin"
        const val CACHE_HISTORY = "history.bin"
        const val CACHE_MEMBER = "member.bin"
    }
}
