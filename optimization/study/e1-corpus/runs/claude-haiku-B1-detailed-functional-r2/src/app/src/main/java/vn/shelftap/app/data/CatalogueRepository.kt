package vn.shelftap.app.data

import android.content.SharedPreferences
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import vn.shelftap.app.data.local.ShelfTapDatabase
import vn.shelftap.app.data.local.TitleEntity
import vn.shelftap.app.data.remote.CatalogueApi
import vn.shelftap.app.model.Title
import java.io.IOException

/**
 * Single source of truth for the UI. Reads always come from the local database,
 * so browsing and searching work offline once the catalogue has been downloaded.
 */
class CatalogueRepository(
    db: ShelfTapDatabase,
    private val api: CatalogueApi,
    private val prefs: SharedPreferences,
) {
    private val dao = db.titleDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null

    private val _sync = MutableStateFlow<SyncState>(
        if (isCatalogueComplete()) SyncState.Complete else SyncState.Idle,
    )
    val sync: StateFlow<SyncState> = _sync.asStateFlow()

    fun pagingAll(): Flow<PagingData<Title>> = pager { dao.pagingAll() }

    /** Titles whose title, author, ISBN or subject contains [query]. An empty query matches everything. */
    fun search(query: String): Flow<PagingData<Title>> {
        val pattern = "%" + escapeLike(query.trim()) + "%"
        return pager { dao.pagingSearch(pattern) }
    }

    suspend fun getTitle(id: String): Title? = dao.getById(id)?.toModel()

    /** Fetches one title from the network and stores it. Throws IOException when offline. */
    suspend fun refreshTitle(id: String): Title = withContext(Dispatchers.IO) {
        val fresh = api.fetchTitle(id)
        dao.upsertAll(listOf(TitleEntity.from(fresh)))
        fresh
    }

    /**
     * Downloads the whole catalogue if it has not been downloaded yet.
     * With [force] = true, downloads it again to pick up changes.
     */
    fun startSyncIfNeeded(force: Boolean = false) {
        if (syncJob?.isActive == true) return
        if (!force && isCatalogueComplete()) return
        syncJob = scope.launch { runFullSync() }
    }

    private suspend fun runFullSync() {
        _sync.value = SyncState.Running(done = 0, total = 0)
        try {
            var page = 0
            var fetched = 0
            var total = Int.MAX_VALUE
            while (fetched < total) {
                val result = api.fetchPage(query = null, page = page, size = CatalogueApi.MAX_PAGE_SIZE)
                if (result.items.isEmpty()) break
                dao.upsertAll(result.items.map { TitleEntity.from(it) })
                total = result.total
                fetched += result.items.size
                page++
                _sync.value = SyncState.Running(done = fetched, total = total)
            }
            prefs.edit().putBoolean(KEY_COMPLETE, true).apply()
            _sync.value = SyncState.Complete
        } catch (e: IOException) {
            _sync.value = SyncState.Failed
        } catch (e: JSONException) {
            _sync.value = SyncState.Failed
        }
    }

    private fun isCatalogueComplete(): Boolean = prefs.getBoolean(KEY_COMPLETE, false)

    private fun pager(source: () -> PagingSource<Int, TitleEntity>): Flow<PagingData<Title>> =
        Pager(PagingConfig(pageSize = PAGE_LIST_SIZE, enablePlaceholders = false)) { source() }
            .flow
            .map { data -> data.map { it.toModel() } }

    companion object {
        private const val PAGE_LIST_SIZE = 40
        private const val KEY_COMPLETE = "catalogue_complete"

        private fun escapeLike(value: String): String =
            value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    }
}

sealed interface SyncState {
    data object Idle : SyncState
    data class Running(val done: Int, val total: Int) : SyncState
    data object Complete : SyncState
    data object Failed : SyncState
}
