package vn.shelftap.app.data.sync

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.shelftap.app.data.local.TitleDao
import vn.shelftap.app.data.local.TitleEntity
import vn.shelftap.app.data.remote.ShelfTapApiService

sealed interface SyncStatus {
    object Idle : SyncStatus
    data class Syncing(val current: Int, val total: Int) : SyncStatus
    data class Completed(val total: Int) : SyncStatus
    data class Error(val message: String, val cachedCount: Int) : SyncStatus
}

class CatalogueSyncManager(
    private val apiService: ShelfTapApiService,
    private val titleDao: TitleDao,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private var syncJob: Job? = null

    fun startSync(force: Boolean = false) {
        if (syncJob?.isActive == true) return

        syncJob = externalScope.launch {
            try {
                val initialCount = titleDao.getTitleCount()
                if (!force && initialCount > 0) {
                    _syncStatus.value = SyncStatus.Completed(initialCount)
                }

                val pageSize = 50
                var currentPage = 0
                var totalItems = 0
                var downloadedCount = 0

                val firstPageResponse = apiService.getTitles(page = 0, size = pageSize)
                totalItems = firstPageResponse.total
                _syncStatus.value = SyncStatus.Syncing(firstPageResponse.items.size, totalItems)

                val firstBatch = firstPageResponse.items.map { TitleEntity.fromDomain(it) }
                titleDao.insertAll(firstBatch)
                downloadedCount += firstBatch.size

                val totalPages = if (totalItems > 0) {
                    (totalItems + pageSize - 1) / pageSize
                } else {
                    1
                }

                currentPage = 1
                while (currentPage < totalPages) {
                    val pageResponse = apiService.getTitles(page = currentPage, size = pageSize)
                    val entities = pageResponse.items.map { TitleEntity.fromDomain(it) }
                    titleDao.insertAll(entities)
                    downloadedCount += entities.size
                    _syncStatus.value = SyncStatus.Syncing(downloadedCount, totalItems)
                    currentPage++
                }

                val finalCount = titleDao.getTitleCount()
                _syncStatus.value = SyncStatus.Completed(finalCount)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("CatalogueSyncManager", "Network error during catalogue sync: ${e.message}")
                val localCount = titleDao.getTitleCount()
                _syncStatus.value = SyncStatus.Error(
                    message = e.localizedMessage ?: "Network connection error",
                    cachedCount = localCount
                )
            }
        }
    }

    fun cancelSync() {
        syncJob?.cancel()
        syncJob = null
        _syncStatus.value = SyncStatus.Idle
    }
}
