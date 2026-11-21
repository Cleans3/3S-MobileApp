package vn.shelftap.app.data

import android.content.SharedPreferences
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.map
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import vn.shelftap.app.data.local.AppDatabase
import vn.shelftap.app.data.local.TitleEntity
import vn.shelftap.app.data.local.toDomain
import vn.shelftap.app.data.local.toEntity
import vn.shelftap.app.data.remote.TitleApi
import vn.shelftap.app.model.Title
import java.io.IOException
import kotlin.math.min

data class SyncProgress(
    val loaded: Int = 0,
    val total: Int = 0,
    val running: Boolean = false,
    val complete: Boolean = false,
    val error: String? = null,
)

/**
 * Single source of truth for the UI. The whole catalogue is downloaded once into Room; after that,
 * browsing and searching read only from the local database and work with no connection.
 */
class CatalogueRepository(
    private val database: AppDatabase,
    private val api: TitleApi,
    private val prefs: SharedPreferences,
    private val appScope: CoroutineScope,
) {
    private val dao = database.titleDao()
    private var syncJob: Job? = null

    private val _sync = MutableStateFlow(initialProgress())
    val sync: StateFlow<SyncProgress> = _sync.asStateFlow()

    /** True once a full download has finished at least once. */
    val isCatalogueReady: Boolean get() = prefs.getBoolean(KEY_COMPLETE, false)

    fun allTitles(): Flow<PagingData<Title>> = pager { dao.pagingAll() }

    fun search(query: String): Flow<PagingData<Title>> {
        val pattern = likePattern(query)
        return pager { dao.pagingSearch(pattern) }
    }

    fun observeTitle(id: String): Flow<Title?> = dao.observeById(id).map { it?.toDomain() }

    /** Starts the full download in the background. Safe to call repeatedly; resumes after a failure. */
    fun startSync() {
        if (isCatalogueReady || syncJob?.isActive == true) return
        syncJob = appScope.launch { runSync() }
    }

    /** Refreshes one title from the API when online. Offline, the cached copy is kept. */
    suspend fun refreshTitle(id: String) {
        try {
            val fresh = withContext(Dispatchers.IO) { api.fetchTitle(id) }
            dao.upsertAll(listOf(fresh.toEntity()))
        } catch (e: IOException) {
            // No connection: keep showing the cached copy.
        } catch (e: JSONException) {
            // Unexpected payload: keep the cached copy.
        }
    }

    private fun pager(source: () -> PagingSource<Int, TitleEntity>): Flow<PagingData<Title>> =
        Pager(
            config = PagingConfig(pageSize = 40, enablePlaceholders = false),
            pagingSourceFactory = source,
        ).flow.map { data -> data.map { it.toDomain() } }

    private fun initialProgress(): SyncProgress {
        val total = prefs.getInt(KEY_TOTAL, 0)
        val loaded = min(prefs.getInt(KEY_NEXT_PAGE, 0) * TitleApi.PAGE_SIZE, total)
        return SyncProgress(loaded = loaded, total = total, complete = isCatalogueReady)
    }

    private suspend fun runSync() {
        _sync.update { it.copy(running = true, error = null) }
        try {
            var page = prefs.getInt(KEY_NEXT_PAGE, 0)
            var total = prefs.getInt(KEY_TOTAL, 0)
            if (page == 0) {
                // Fresh download: drop any stale rows first.
                database.withTransaction { dao.deleteAll() }
                total = 0
            }
            while (true) {
                val result = withContext(Dispatchers.IO) { api.fetchPage(page, TitleApi.PAGE_SIZE) }
                database.withTransaction { dao.upsertAll(result.items.map { it.toEntity() }) }
                page++
                total = result.total
                // Saved after each page so an interrupted download resumes where it stopped.
                prefs.edit().putInt(KEY_NEXT_PAGE, page).putInt(KEY_TOTAL, total).apply()
                _sync.update {
                    SyncProgress(
                        loaded = min(page * TitleApi.PAGE_SIZE, total),
                        total = total,
                        running = true,
                    )
                }
                if (result.items.isEmpty() || page * TitleApi.PAGE_SIZE >= total) break
            }
            prefs.edit()
                .putBoolean(KEY_COMPLETE, true)
                .remove(KEY_NEXT_PAGE)
                .remove(KEY_TOTAL)
                .apply()
            _sync.value = SyncProgress(loaded = total, total = total, running = false, complete = true)
        } catch (e: IOException) {
            _sync.update { it.copy(running = false, error = ERROR_MESSAGE) }
        } catch (e: JSONException) {
            _sync.update { it.copy(running = false, error = ERROR_MESSAGE) }
        }
    }

    companion object {
        private const val KEY_COMPLETE = "complete"
        private const val KEY_NEXT_PAGE = "next_page"
        private const val KEY_TOTAL = "total"
        private const val ERROR_MESSAGE =
            "The download stopped because the connection failed. Tap Retry to continue."

        /**
         * Turns user input into a LIKE pattern: lowercased (for Unicode text, where SQLite LIKE is
         * case-sensitive), trimmed, with the wildcard characters escaped using '!'.
         */
        internal fun likePattern(query: String): String =
            query.trim().lowercase()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
    }
}
