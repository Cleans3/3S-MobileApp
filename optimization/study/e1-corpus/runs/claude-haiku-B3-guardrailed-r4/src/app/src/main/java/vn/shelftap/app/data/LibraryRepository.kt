package vn.shelftap.app.data

import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** A value plus whether it came from the encrypted local copy because the network was unavailable. */
class Loaded<T>(val value: T, val fromCache: Boolean)

data class HistorySnapshot(val historyEnabled: Boolean, val items: List<HistoryItem>)

/**
 * Combines the API with the encrypted local copy. A successful network read refreshes the copy;
 * when the network is unavailable the last saved copy is returned instead.
 */
class LibraryRepository(
    private val api: ShelfTapApi,
    private val cache: EncryptedCache,
    private val ioDispatcher: CoroutineDispatcher,
) {
    /** Throws an IOException when offline and nothing has been saved yet. */
    suspend fun loans(): Loaded<List<Loan>> = withContext(ioDispatcher) {
        try {
            val loans = api.getLoans()
            saveLoans(loans)
            Loaded(loans, fromCache = false)
        } catch (e: IOException) {
            val saved = readSavedLoans() ?: throw e
            Loaded(saved, fromCache = true)
        }
    }

    /** Renews a loan online and updates the saved copy. */
    suspend fun renew(loanId: String): Loan = withContext(ioDispatcher) {
        val updated = api.renewLoan(loanId)
        readSavedLoans()?.let { saved ->
            saveLoans(saved.map { if (it.loanId == updated.loanId) updated else it })
        }
        updated
    }

    /** Throws an IOException when offline and nothing has been saved yet. */
    suspend fun history(): Loaded<HistorySnapshot> = withContext(ioDispatcher) {
        try {
            val member = api.getMember()
            val snapshot = HistorySnapshot(member.historyEnabled, api.getHistory())
            saveHistory(snapshot)
            Loaded(snapshot, fromCache = false)
        } catch (e: IOException) {
            val saved = readSavedHistory() ?: throw e
            Loaded(saved, fromCache = true)
        }
    }

    /**
     * Changes the reading-history setting. When it is switched off, the saved history is replaced
     * with an empty one so nothing is kept on the phone.
     */
    suspend fun setHistoryEnabled(enabled: Boolean): HistorySnapshot = withContext(ioDispatcher) {
        val member = api.updatePrivacy(enabled)
        val items = if (member.historyEnabled) api.getHistory() else emptyList()
        HistorySnapshot(member.historyEnabled, items).also { saveHistory(it) }
    }

    suspend fun title(titleId: String): CatalogTitle = withContext(ioDispatcher) {
        api.getTitle(titleId)
    }

    private fun saveLoans(loans: List<Loan>) {
        cache.write(LOANS_CACHE, JSONArray(loans.map { it.toJson() }).toString())
    }

    private fun readSavedLoans(): List<Loan>? {
        val text = cache.read(LOANS_CACHE) ?: return null
        return try {
            JSONArray(text).mapObjects(::loanFromJson)
        } catch (e: JSONException) {
            null
        }
    }

    private fun saveHistory(snapshot: HistorySnapshot) {
        val json = JSONObject()
            .put("historyEnabled", snapshot.historyEnabled)
            .put("items", JSONArray(snapshot.items.map { it.toJson() }))
        cache.write(HISTORY_CACHE, json.toString())
    }

    private fun readSavedHistory(): HistorySnapshot? {
        val text = cache.read(HISTORY_CACHE) ?: return null
        return try {
            val json = JSONObject(text)
            HistorySnapshot(
                historyEnabled = json.getBoolean("historyEnabled"),
                items = json.getJSONArray("items").mapObjects(::historyItemFromJson),
            )
        } catch (e: JSONException) {
            null
        }
    }

    private companion object {
        const val LOANS_CACHE = "loans"
        const val HISTORY_CACHE = "history"
    }
}
