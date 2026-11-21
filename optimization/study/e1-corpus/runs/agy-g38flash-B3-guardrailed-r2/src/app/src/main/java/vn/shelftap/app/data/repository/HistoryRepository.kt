package vn.shelftap.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.shelftap.app.data.api.ShelfTapApi
import vn.shelftap.app.data.crypto.EncryptedStorageManager
import vn.shelftap.app.data.model.HistoryItem

sealed class HistoryResult {
    data class Success(val history: List<HistoryItem>, val isSavedData: Boolean) : HistoryResult()
    data class Error(val message: String, val fallbackHistory: List<HistoryItem>? = null) : HistoryResult()
}

class HistoryRepository(
    private val api: ShelfTapApi,
    private val encryptedStorage: EncryptedStorageManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun getHistory(): HistoryResult = withContext(ioDispatcher) {
        try {
            val networkHistory = api.getHistory()
            encryptedStorage.saveHistory(networkHistory)
            HistoryResult.Success(history = networkHistory, isSavedData = false)
        } catch (e: Exception) {
            val cached = encryptedStorage.getHistory()
            if (cached != null) {
                HistoryResult.Success(history = cached, isSavedData = true)
            } else {
                HistoryResult.Error(message = e.localizedMessage ?: "Failed to fetch history")
            }
        }
    }
}
