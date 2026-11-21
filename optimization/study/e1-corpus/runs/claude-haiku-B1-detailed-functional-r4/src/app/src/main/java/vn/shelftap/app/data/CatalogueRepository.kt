package vn.shelftap.app.data

import android.content.SharedPreferences
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Single source of truth for the UI. The UI reads only from the local database, so browsing and
 * searching keep working offline once the catalogue has been downloaded once.
 */
class CatalogueRepository(
    private val api: ShelfTapApi,
    private val dao: TitleDao,
    private val prefs: SharedPreferences,
    private val scope: CoroutineScope,
) {
    private val syncMutable = MutableStateFlow<SyncState>(
        if (isCatalogueDownloaded) SyncState.Complete else SyncState.Idle,
    )
    val syncState: StateFlow<SyncState> = syncMutable.asStateFlow()

    private var syncJob: Job? = null

    val isCatalogueDownloaded: Boolean
        get() = prefs.getBoolean(KEY_COMPLETE, false)

    /** Downloads the whole catalogue on first launch. Interrupted downloads restart on next launch. */
    fun startSyncIfNeeded() {
        if (!isCatalogueDownloaded) startSync()
    }

    /** Starts the download unless it is already running or has finished. */
    @Synchronized
    fun startSync() {
        if (isCatalogueDownloaded || syncJob?.isActive == true) return
        syncJob = scope.launch { runFullSync() }
    }

    private suspend fun runFullSync() {
        syncMutable.value = SyncState.Running(fetched = 0, total = 0)
        try {
            var page = 0
            var fetched = 0
            while (true) {
                val response = fetchPageWithRetry(page)
                if (response.items.isNotEmpty()) {
                    dao.upsertAll(response.items.map { it.toEntity() })
                }
                fetched += response.items.size
                syncMutable.value = SyncState.Running(fetched = fetched, total = response.total)
                if (response.items.isEmpty() || fetched >= response.total) break
                page++
            }
            prefs.edit().putBoolean(KEY_COMPLETE, true).apply()
            syncMutable.value = SyncState.Complete
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            syncMutable.value = SyncState.Failed("Catalogue download did not finish.")
        }
    }

    private suspend fun fetchPageWithRetry(page: Int): TitlePageDto {
        var attempt = 1
        while (true) {
            try {
                return api.listTitles(page = page, size = PAGE_SIZE)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (attempt >= MAX_ATTEMPTS) throw e
                delay(RETRY_BASE_DELAY_MS * attempt)
                attempt++
            }
        }
    }

    /** Refreshes one title from the server. Callers should ignore failures and keep the local copy. */
    suspend fun refreshTitle(id: String) {
        dao.upsertAll(listOf(api.getTitle(id).toEntity()))
    }

    fun observeTitle(id: String): Flow<TitleEntity?> = dao.observeById(id)

    fun pagedTitles(): Flow<PagingData<TitleEntity>> = pager { dao.pagingAll() }

    fun pagedSearch(query: String): Flow<PagingData<TitleEntity>> {
        val pattern = SearchText.likePattern(query)
        return pager { dao.pagingSearch(pattern) }
    }

    private fun pager(source: () -> PagingSource<Int, TitleEntity>): Flow<PagingData<TitleEntity>> =
        Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = source,
        ).flow

    companion object {
        const val PAGE_SIZE = 50
        private const val MAX_ATTEMPTS = 4
        private const val RETRY_BASE_DELAY_MS = 2_000L
        private const val KEY_COMPLETE = "catalogue_complete"
    }
}
