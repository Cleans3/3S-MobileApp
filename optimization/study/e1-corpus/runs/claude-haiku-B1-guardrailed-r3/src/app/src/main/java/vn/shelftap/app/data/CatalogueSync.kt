package vn.shelftap.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONException
import vn.shelftap.app.data.local.AppDatabase
import vn.shelftap.app.data.local.SyncStateEntity
import vn.shelftap.app.data.remote.CatalogueApi
import java.io.IOException

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data class Running(val loaded: Int, val total: Int) : SyncStatus
    data object Complete : SyncStatus
    data object Paused : SyncStatus
}

/**
 * Downloads the whole catalogue once, one page at a time. Each page and the saved
 * "next page" marker are written in one transaction, so an interrupted download resumes
 * from the last committed page and never stores a page twice.
 */
class CatalogueSync(
    private val database: AppDatabase,
    private val api: CatalogueApi,
    private val scope: CoroutineScope,
) {
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch { runSync() }
    }

    private suspend fun runSync() {
        val stateDao = database.syncStateDao()
        var state = stateDao.get() ?: SyncStateEntity(id = 0, nextPage = 0, total = 0, complete = false)
        if (state.complete) {
            _status.value = SyncStatus.Complete
            return
        }
        try {
            while (true) {
                _status.value = SyncStatus.Running(loaded = state.nextPage * PAGE_SIZE, total = state.total)
                val page = api.listPage(state.nextPage, PAGE_SIZE)
                val done = page.items.isEmpty() || (state.nextPage + 1) * PAGE_SIZE >= page.total
                val next = state.copy(nextPage = state.nextPage + 1, total = page.total, complete = done)
                database.withTransaction {
                    database.titleDao().insertAll(page.items.map { it.toEntity() })
                    database.titleSearchDao().insertAll(page.items.map { it.toSearchEntity() })
                    stateDao.save(next)
                }
                state = next
                if (done) break
            }
            _status.value = SyncStatus.Complete
        } catch (e: IOException) {
            _status.value = SyncStatus.Paused
        } catch (e: JSONException) {
            _status.value = SyncStatus.Paused
        }
    }

    companion object {
        const val PAGE_SIZE = 50
    }
}
