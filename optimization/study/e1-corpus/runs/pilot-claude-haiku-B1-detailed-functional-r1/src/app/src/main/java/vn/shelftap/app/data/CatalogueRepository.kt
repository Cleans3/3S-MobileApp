package vn.shelftap.app.data

import android.content.SharedPreferences
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import vn.shelftap.app.data.db.AppDatabase
import vn.shelftap.app.data.db.TitleEntity
import vn.shelftap.app.data.remote.CatalogueApi
import vn.shelftap.app.data.remote.TitleDto

sealed interface SyncState {
    data object Idle : SyncState
    data class Running(val done: Int, val total: Int) : SyncState
    data object Done : SyncState
    data object Failed : SyncState
}

/**
 * Single source of truth for the UI. Everything the UI shows comes from the local
 * Room database, so browsing and searching work offline once the catalogue is stored.
 * The network is only used to download the catalogue and to fetch a title that is
 * not stored yet.
 */
class CatalogueRepository(
    private val db: AppDatabase,
    private val api: CatalogueApi,
    private val prefs: SharedPreferences,
    private val appScope: CoroutineScope,
) {
    private val dao = db.titleDao()
    private val syncLock = Mutex()

    private val _sync = MutableStateFlow<SyncState>(SyncState.Idle)
    val sync: StateFlow<SyncState> = _sync.asStateFlow()

    fun catalogue(): Flow<PagingData<TitleEntity>> = pager { dao.pageAll() }

    fun search(query: String): Flow<PagingData<TitleEntity>> {
        val pattern = likePattern(query)
        return pager { dao.pageSearch(pattern) }
    }

    /** Returns the title from the local store, or fetches it once from the API. */
    suspend fun title(id: String): TitleEntity? {
        dao.byId(id)?.let { return it }
        val dto = api.title(id) ?: return null
        val entity = dto.toEntity()
        dao.upsertAll(listOf(entity))
        return entity
    }

    fun startSyncIfIncomplete() {
        if (!prefs.getBoolean(KEY_COMPLETE, false)) startSync()
    }

    fun startSync() {
        appScope.launch { syncAll() }
    }

    /** Downloads every page of the catalogue and stores it. A second call while one runs is ignored. */
    suspend fun syncAll() {
        if (!syncLock.tryLock()) return
        try {
            _sync.value = SyncState.Running(0, 0)
            var page = 0
            var fetched = 0
            var total = 0
            do {
                val response = api.titles(page, PAGE_SIZE)
                dao.upsertAll(response.items.map { it.toEntity() })
                fetched += response.items.size
                total = response.total
                _sync.value = SyncState.Running(fetched, total)
                page++
            } while (response.items.isNotEmpty() && fetched < total)
            prefs.edit().putBoolean(KEY_COMPLETE, true).apply()
            _sync.value = SyncState.Done
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _sync.value = SyncState.Failed
        } finally {
            syncLock.unlock()
        }
    }

    private fun pager(source: () -> PagingSource<Int, TitleEntity>): Flow<PagingData<TitleEntity>> =
        Pager(
            config = PagingConfig(pageSize = UI_PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = source,
        ).flow

    companion object {
        /** Largest page size the API accepts. */
        const val PAGE_SIZE = 50
        private const val UI_PAGE_SIZE = 30
        private const val KEY_COMPLETE = "catalogue_complete"

        /**
         * Builds a SQL LIKE pattern that matches the lower-cased text containing [query].
         * The characters %, _ and ! are escaped with '!' (see ESCAPE in the DAO).
         */
        fun likePattern(query: String): String {
            val escaped = query.trim().lowercase()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
            return "%$escaped%"
        }
    }
}

private fun TitleDto.toEntity(): TitleEntity = TitleEntity(
    id = id,
    isbn = isbn,
    title = title,
    authors = authors,
    year = year,
    subjects = subjects,
    coverUrl = coverUrl?.takeIf { it.isNotBlank() },
    copiesAvailable = copiesAvailable,
    // Title, ISBN, authors and subjects are all searched through this one column.
    searchText = listOf(title, isbn, authors.joinToString(" "), subjects.joinToString(" "))
        .joinToString("\n")
        .lowercase(),
)
