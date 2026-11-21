package vn.shelftap.app.data

import android.content.SharedPreferences
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.Pager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single source of titles for the UI. The UI only reads from the local database, so
 * browsing and searching work offline once the catalogue has been downloaded.
 */
class CatalogueRepository(
    private val api: ShelfTapApi,
    private val dao: TitleDao,
    private val prefs: SharedPreferences,
) {
    private val syncMutex = Mutex()
    private val _sync = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val sync: StateFlow<SyncStatus> = _sync.asStateFlow()

    /** All titles, sorted by title. Pages are read from the local database. */
    fun pagingAll(): Flow<PagingData<TitleEntity>> = pager { dao.pagingAll() }

    /** Titles whose title, author, ISBN or subject contains [query] (case-insensitive). */
    fun pagingSearch(query: String): Flow<PagingData<TitleEntity>> {
        val pattern = escapeLike(query.trim().lowercase())
        return pager { dao.pagingSearch(pattern) }
    }

    /** Returns a title from the local store, or fetches it once from the API if missing. */
    suspend fun title(id: String): TitleEntity? {
        dao.getById(id)?.let { return it }
        return try {
            api.title(id).toEntity(System.currentTimeMillis()).also { dao.upsert(listOf(it)) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /** Downloads the catalogue if it was never downloaded or the last download is stale. */
    suspend fun syncIfStale(maxAgeMillis: Long = MAX_AGE_MILLIS) {
        val last = prefs.getLong(KEY_LAST_SYNC, 0L)
        if (last > 0L && System.currentTimeMillis() - last < maxAgeMillis) {
            _sync.value = SyncStatus.Done
        } else {
            syncAll()
        }
    }

    /** Downloads every page of the catalogue and stores it. Runs one at a time. */
    suspend fun syncAll() {
        syncMutex.withLock {
            val runStart = System.currentTimeMillis()
            _sync.value = SyncStatus.Running(0, 0)
            try {
                var page = 0
                var loaded = 0
                do {
                    val response = api.titles(q = null, page = page, size = PAGE_SIZE)
                    if (response.items.isEmpty()) break
                    dao.upsert(response.items.map { it.toEntity(runStart) })
                    loaded += response.items.size
                    page++
                    _sync.value = SyncStatus.Running(loaded, response.total)
                } while (loaded < response.total)

                // Titles not seen in this complete run were removed from the catalogue.
                dao.deleteNotSyncedSince(runStart)
                prefs.edit().putLong(KEY_LAST_SYNC, runStart).apply()
                _sync.value = SyncStatus.Done
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _sync.value = SyncStatus.Failed
            }
        }
    }

    private fun pager(source: () -> PagingSource<Int, TitleEntity>): Flow<PagingData<TitleEntity>> =
        Pager(PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false)) { source() }.flow

    private fun escapeLike(text: String): String =
        text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    companion object {
        const val PAGE_SIZE = 50
        private const val KEY_LAST_SYNC = "last_full_sync"
        private const val MAX_AGE_MILLIS = 24L * 60 * 60 * 1000
    }
}
