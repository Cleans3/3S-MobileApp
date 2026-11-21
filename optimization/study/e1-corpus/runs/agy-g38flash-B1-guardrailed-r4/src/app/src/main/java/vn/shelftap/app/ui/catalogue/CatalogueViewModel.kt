package vn.shelftap.app.ui.catalogue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import vn.shelftap.app.data.model.Title
import vn.shelftap.app.data.repository.CatalogueRepository

sealed interface SyncState {
    data object Idle : SyncState
    data class Syncing(val downloaded: Int, val total: Int) : SyncState
    data class Success(val count: Int) : SyncState
    data class Error(val message: String) : SyncState
}

/**
 * ViewModel for Catalogue & Search.
 * Adheres to Rule 14 (expensive operations in ViewModel)
 * and Rule 17 (debounced search with flatMapLatest against indexed local store).
 */
class CatalogueViewModel(
    private val repository: CatalogueRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val titlesFlow: Flow<PagingData<Title>> = _searchQuery
        .debounce(300L)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            repository.getTitlesPaged(query)
        }
        .cachedIn(viewModelScope)

    init {
        checkAndSyncIfNeeded()
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun syncCatalogue() {
        if (_syncState.value is SyncState.Syncing) return

        viewModelScope.launch(ioDispatcher) {
            _syncState.value = SyncState.Syncing(0, 0)
            val result = repository.syncCatalogue { downloaded, total ->
                _syncState.value = SyncState.Syncing(downloaded, total)
            }
            _syncState.value = result.fold(
                onSuccess = { SyncState.Success(it) },
                onFailure = { SyncState.Error(it.message ?: "Sync failed") }
            )
        }
    }

    private fun checkAndSyncIfNeeded() {
        viewModelScope.launch(ioDispatcher) {
            val count = repository.getLocalTitleCount()
            if (count == 0) {
                // If local database is empty, trigger initial catalogue download
                syncCatalogue()
            }
        }
    }
}
