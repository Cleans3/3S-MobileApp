package vn.shelftap.app.data

import vn.shelftap.app.model.HistoryItem
import vn.shelftap.app.model.HistorySnapshot
import vn.shelftap.app.model.Loaded
import vn.shelftap.app.model.Loan
import vn.shelftap.app.model.Member
import vn.shelftap.app.model.TitleDetail
import java.io.IOException

/**
 * Loans, history and book data. Loans and history fall back to the encrypted
 * local copy when the network is unavailable or the server fails (5xx).
 */
class LibraryRepository(
    private val api: ShelfTapApi,
    private val store: SavedDataStore,
) {

    suspend fun loans(): Loaded<List<Loan>> = withSavedFallback(
        remote = {
            val fresh = api.myLoans()
            store.update { it.copy(loans = fresh) }
            fresh
        },
        saved = { store.read().loans },
    )

    suspend fun history(): Loaded<HistorySnapshot> = withSavedFallback(
        remote = {
            val member = api.me()
            val items = if (member.historyEnabled) api.history() else emptyList()
            store.update { it.copy(history = items, historyEnabled = member.historyEnabled) }
            historySnapshot(items, member.historyEnabled)
        },
        saved = {
            val saved = store.read()
            val items = saved.history
            if (items == null) null else historySnapshot(items, saved.historyEnabled ?: true)
        },
    )

    suspend fun renew(loanId: String): Loan {
        val updated = api.renewLoan(loanId)
        store.update { saved ->
            saved.copy(loans = saved.loans?.map { if (it.loanId == loanId) updated else it })
        }
        return updated
    }

    suspend fun setHistoryEnabled(enabled: Boolean): Member {
        val member = api.setHistoryEnabled(enabled)
        store.update { saved ->
            saved.copy(
                historyEnabled = member.historyEnabled,
                history = if (member.historyEnabled) saved.history else emptyList(),
            )
        }
        return member
    }

    suspend fun title(titleId: String): TitleDetail = api.title(titleId)

    private fun historySnapshot(items: List<HistoryItem>, enabled: Boolean): HistorySnapshot =
        HistorySnapshot(items = items.sortedByDescending { it.outAt }, historyEnabled = enabled)

    private suspend fun <T> withSavedFallback(
        remote: suspend () -> T,
        saved: suspend () -> T?,
    ): Loaded<T> = try {
        Loaded(remote(), fromSavedData = false)
    } catch (e: Exception) {
        if (!e.allowsSavedFallback()) throw e
        Loaded(saved() ?: throw e, fromSavedData = true)
    }

    private fun Exception.allowsSavedFallback(): Boolean =
        this is IOException || (this is ApiException && status >= 500)
}
