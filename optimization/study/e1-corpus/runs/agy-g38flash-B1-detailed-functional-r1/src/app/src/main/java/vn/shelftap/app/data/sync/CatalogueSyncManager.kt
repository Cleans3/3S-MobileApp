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
import kotlinx.coroutines.withContext
import vn.shelftap.app.data.local.TitleDao
import vn.shelftap.app.data.local.TitleEntity
import vn.shelftap.app.data.remote.ShelfTapApiService

/**
 * Manages background downloading and synchronizing the entire 100,000-title catalogue
 * into the local SQLite/Room database so members can browse and search completely offline.
 */
class CatalogueSyncManager(
    private val apiService: ShelfTapApiService,
    private val titleDao: TitleDao
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var syncJob: Job? = null

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    /**
     * Start downloading or updating the full catalogue from https://api.shelftap.example.
     */
    fun startSync(batchSize: Int = 50) {
        if (syncJob?.isActive == true) return

        syncJob = scope.launch {
            try {
                var currentPage = 0
                var downloadedCount = 0
                var totalTitles = 0

                // Fetch page 0 to learn the total catalogue count
                val firstPage = apiService.getTitles(page = 0, size = batchSize)
                totalTitles = firstPage.total
                val firstBatchEntities = firstPage.items.map { TitleEntity.fromDomain(it) }
                titleDao.insertTitles(firstBatchEntities)
                downloadedCount += firstBatchEntities.size

                _syncStatus.value = SyncStatus.Syncing(
                    downloaded = downloadedCount,
                    total = totalTitles
                )

                val totalPages = if (totalTitles > 0) {
                    (totalTitles + batchSize - 1) / batchSize
                } else {
                    1
                }

                // Download subsequent pages
                for (page in 1 until totalPages) {
                    val pageResponse = apiService.getTitles(page = page, size = batchSize)
                    if (pageResponse.items.isEmpty()) break

                    val entities = pageResponse.items.map { TitleEntity.fromDomain(it) }
                    titleDao.insertTitles(entities)
                    downloadedCount += entities.size

                    _syncStatus.value = SyncStatus.Syncing(
                        downloaded = downloadedCount,
                        total = totalTitles
                    )
                }

                _syncStatus.value = SyncStatus.Completed(totalTitles = downloadedCount)
            } catch (c: CancellationException) {
                Log.d("CatalogueSync", "Sync cancelled")
                throw c
            } catch (e: Exception) {
                Log.w("CatalogueSync", "Network or sync error", e)
                val currentLocalCount = titleDao.getTitleCount()
                _syncStatus.value = SyncStatus.Error(
                    message = e.localizedMessage ?: "Network connection unavailable",
                    downloadedSoFar = currentLocalCount
                )
            }
        }
    }

    /**
     * Cancel an active sync job.
     */
    fun cancelSync() {
        syncJob?.cancel()
        syncJob = null
        _syncStatus.value = SyncStatus.Idle
    }
}
