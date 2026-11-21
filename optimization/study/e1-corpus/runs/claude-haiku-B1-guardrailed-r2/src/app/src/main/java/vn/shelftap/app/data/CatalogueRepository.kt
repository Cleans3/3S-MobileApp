package vn.shelftap.app.data

import androidx.paging.PagingSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import java.io.IOException

sealed interface SyncState {
    data object Idle : SyncState
    data class Running(val done: Int, val total: Int) : SyncState
    data object Complete : SyncState
    data object Failed : SyncState
}

/**
 * Offline-first access to the catalogue. The full catalogue is downloaded once into Room;
 * after that, browsing and searching read only from the local database.
 */
class CatalogueRepository(
    private val api: TitleApi,
    private val dao: TitleDao,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher
) {
    private val syncMutex = Mutex()
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    fun pagingAll(): PagingSource<Int, TitleEntity> = dao.pagingAll()

    fun pagingSearch(match: String): PagingSource<Int, TitleEntity> = dao.pagingSearch(match)

    /** Local copy first; the network is used only if the title is not in the local catalogue. */
    suspend fun getTitle(id: String): TitleEntity = withContext(ioDispatcher) {
        dao.findById(id) ?: api.fetchTitle(id).toEntity()
    }

    /** Starts the download in the background. Does nothing when it is already complete. */
    fun startSync() {
        scope.launch { syncCatalogue() }
    }

    /**
     * Downloads every page from the last committed position. Safe to call again after a failure:
     * it resumes where it stopped. Network failures set [SyncState.Failed] instead of throwing.
     */
    suspend fun syncCatalogue() = withContext(ioDispatcher) {
        syncMutex.withLock {
            var state = dao.syncState() ?: SyncStateEntity(nextPage = 0, total = 0, complete = false)
            if (state.complete) {
                _syncState.value = SyncState.Complete
                return@withLock
            }
            if (state.nextPage == 0) dao.resetCatalogue()
            publishProgress(state)

            try {
                while (true) {
                    val page = api.fetchPage(state.nextPage, PAGE_SIZE)
                    val nextPage = state.nextPage + 1
                    val finished = page.items.isEmpty() || nextPage * PAGE_SIZE >= page.total
                    val newState = SyncStateEntity(
                        nextPage = nextPage,
                        total = page.total,
                        complete = finished
                    )
                    dao.savePage(
                        titles = page.items.map { it.toEntity() },
                        searchRows = page.items.map { it.toSearchRow() },
                        state = newState
                    )
                    state = newState
                    publishProgress(state)
                    if (finished) break
                }
            } catch (e: IOException) {
                _syncState.value = SyncState.Failed
            } catch (e: SerializationException) {
                _syncState.value = SyncState.Failed
            }
        }
    }

    private fun publishProgress(state: SyncStateEntity) {
        _syncState.value = if (state.complete) {
            SyncState.Complete
        } else {
            SyncState.Running(
                done = minOf(state.nextPage * PAGE_SIZE, state.total),
                total = state.total
            )
        }
    }

    companion object {
        /** Page size for both the API download and the on-screen lists. The API allows at most 50. */
        const val PAGE_SIZE = 50
    }
}

private fun TitleDto.toEntity() = TitleEntity(
    id = id,
    isbn = isbn,
    title = title,
    sortTitle = title.lowercase(),
    authors = authors,
    year = year,
    subjects = subjects,
    coverUrl = coverUrl,
    copiesAvailable = copiesAvailable
)

private fun TitleDto.toSearchRow() = TitleSearchEntity(
    id = id,
    title = title,
    authors = authors.joinToString(" "),
    isbn = isbn,
    subjects = subjects.joinToString(" ")
)
