package vn.shelftap.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.shelftap.app.data.api.ShelfTapApiService
import vn.shelftap.app.data.local.ShelfTapDatabaseHelper
import vn.shelftap.app.data.model.HistoryItem
import vn.shelftap.app.util.DateUtils

class HistoryRepository(
    private val apiService: ShelfTapApiService,
    private val dbHelper: ShelfTapDatabaseHelper
) {

    /**
     * Loads history items sorted newest first.
     * If network fails or is unavailable, falls back to locally saved history.
     * Returns Pair<List<HistoryItem>, Boolean> where the Boolean is true if data is from local cache.
     */
    suspend fun getHistory(): Result<Pair<List<HistoryItem>, Boolean>> = withContext(Dispatchers.IO) {
        try {
            val remoteHistory = apiService.getHistory()
            val sorted = sortNewestFirst(remoteHistory)
            dbHelper.saveHistory(sorted)
            Result.success(Pair(sorted, false))
        } catch (e: Exception) {
            val cachedHistory = dbHelper.getHistory()
            if (cachedHistory.isNotEmpty()) {
                val sorted = sortNewestFirst(cachedHistory)
                Result.success(Pair(sorted, true))
            } else {
                Result.failure(e)
            }
        }
    }

    private fun sortNewestFirst(items: List<HistoryItem>): List<HistoryItem> {
        return items.sortedWith(
            compareByDescending<HistoryItem> {
                DateUtils.parseIsoInstant(it.returnedAt) ?: DateUtils.parseIsoInstant(it.outAt)
            }
        )
    }
}
