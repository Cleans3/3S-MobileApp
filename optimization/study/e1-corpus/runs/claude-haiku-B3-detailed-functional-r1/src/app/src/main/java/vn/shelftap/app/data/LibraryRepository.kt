package vn.shelftap.app.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import java.io.IOException

/** Result of a read: [isSaved] is true when the network failed and the data came from the device. */
data class Snapshot<T>(val data: T, val isSaved: Boolean)

class LibraryRepository(
    private val api: ShelfTapApi,
    private val store: SavedDataStore,
) {
    private val loansSerializer = ListSerializer(Loan.serializer())
    private val historySerializer = ListSerializer(HistoryItem.serializer())

    suspend fun loans(): Snapshot<List<Loan>> =
        fetchOrSaved(KEY_LOANS, loansSerializer) { api.loans() }

    suspend fun history(): Snapshot<List<HistoryItem>> =
        fetchOrSaved(KEY_HISTORY, historySerializer) { api.history() }

    suspend fun member(): Snapshot<Member> =
        fetchOrSaved(KEY_MEMBER, Member.serializer()) { api.member() }

    suspend fun title(titleId: String): BookTitle = api.title(titleId)

    /** Renews a loan and updates the saved loan list so the new due date survives offline use. */
    suspend fun renew(loanId: String): Loan {
        val updated = api.renewLoan(loanId)
        val saved = store.load(KEY_LOANS, loansSerializer).orEmpty()
        store.save(KEY_LOANS, loansSerializer, saved.map { if (it.loanId == loanId) updated else it })
        return updated
    }

    suspend fun setHistoryEnabled(enabled: Boolean): Member {
        val updated = api.setHistoryEnabled(enabled)
        store.save(KEY_MEMBER, Member.serializer(), updated)
        return updated
    }

    /**
     * Fetches from the network and saves the result. If the network is unavailable, returns the
     * last saved value marked as saved; if nothing was saved, rethrows the network error.
     */
    private suspend fun <T> fetchOrSaved(
        key: String,
        serializer: KSerializer<T>,
        fetch: suspend () -> T,
    ): Snapshot<T> {
        return try {
            val fresh = fetch()
            store.save(key, serializer, fresh)
            Snapshot(fresh, isSaved = false)
        } catch (e: IOException) {
            val saved = store.load(key, serializer) ?: throw e
            Snapshot(saved, isSaved = true)
        }
    }

    private companion object {
        const val KEY_LOANS = "loans"
        const val KEY_HISTORY = "history"
        const val KEY_MEMBER = "member"
    }
}
