package vn.shelftap.app.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.map
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import vn.shelftap.app.data.local.ShelfTapDatabase
import vn.shelftap.app.data.local.SyncStateEntity
import vn.shelftap.app.data.local.TitleEntity
import vn.shelftap.app.data.remote.CatalogueApi
import vn.shelftap.app.domain.Title
import java.io.IOException
import kotlin.math.min

/**
 * Single source of truth for the UI. The local Room database is always read; the network
 * is only used to fill it (full catalogue download, and single-title refresh).
 */
class CatalogueRepository(
    private val database: ShelfTapDatabase,
    private val api: CatalogueApi,
    private val ioDispatcher: CoroutineDispatcher,
    private val appScope: CoroutineScope,
) {
    private val dao = database.titleDao()
    private val syncMutex = Mutex()
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    /** Starts downloading the catalogue unless it is already complete or a download is running. */
    fun startSync() {
        appScope.launch { runSync() }
    }

    private suspend fun runSync() {
        syncMutex.withLock {
            withContext(ioDispatcher) {
                _syncStatus.value = try {
                    syncPages()
                    SyncStatus.Complete
                } catch (e: IOException) {
                    SyncStatus.Failed
                } catch (e: SerializationException) {
                    SyncStatus.Failed
                }
            }
        }
    }

    /** Stores every page not yet saved. A failure keeps the saved page, so the next run resumes. */
    private suspend fun syncPages() {
        val saved = dao.getSyncState()
        if (saved?.complete == true) return

        var page = saved?.nextPage ?: 0
        var total = saved?.total ?: 0
        _syncStatus.value = SyncStatus.Running(loaded = page * PAGE_SIZE, total = total)

        while (true) {
            val response = api.listTitles(page = page, size = PAGE_SIZE)
            total = response.total
            if (response.items.isEmpty()) break

            val titles = response.items.map { it.toEntity() }
            val ids = titles.map { it.id }
            val nextPage = page + 1
            database.withTransaction {
                dao.upsertTitles(titles)
                dao.deleteSearchRows(ids)
                dao.insertSearchRows(ids)
                dao.saveSyncState(SyncStateEntity(id = 1, nextPage = nextPage, total = total, complete = false))
            }

            page = nextPage
            _syncStatus.value = SyncStatus.Running(loaded = min(page * PAGE_SIZE, total), total = total)
            if (page * PAGE_SIZE >= total) break
        }

        dao.saveSyncState(SyncStateEntity(id = 1, nextPage = page, total = total, complete = true))
    }

    /** Catalogue in title order, read from the local database. */
    fun catalogue(): Flow<PagingData<Title>> = pagedTitles { dao.pagingCatalogue() }

    /** Titles matching a prepared FTS MATCH expression (see [buildSearchMatch]). */
    fun search(match: String): Flow<PagingData<Title>> = pagedTitles { dao.pagingSearch(match) }

    fun observeTitle(id: String): Flow<Title?> = dao.observeById(id).map { entity -> entity?.toDomain() }

    /** Fetches one title and stores it. Returns false when the network call fails. */
    suspend fun refreshTitle(id: String): Boolean = withContext(ioDispatcher) {
        try {
            val entity = api.getTitle(id).toEntity()
            database.withTransaction {
                dao.upsertTitles(listOf(entity))
                dao.deleteSearchRows(listOf(entity.id))
                dao.insertSearchRows(listOf(entity.id))
            }
            true
        } catch (e: IOException) {
            false
        } catch (e: SerializationException) {
            false
        }
    }

    private fun pagedTitles(source: () -> PagingSource<Int, TitleEntity>): Flow<PagingData<Title>> =
        Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = source,
        ).flow.map { pagingData -> pagingData.map { it.toDomain() } }

    companion object {
        const val PAGE_SIZE = 50
    }
}
