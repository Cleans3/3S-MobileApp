package vn.shelftap.app.ui.catalogue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
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

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class CatalogueViewModel(
    private val repository: CatalogueRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _syncState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

    // Rule 17: Debounce query flow, cancel previous query on new one arriving (flatMapLatest),
    // query Room @Fts4 off main thread, and page results
    val titles: Flow<PagingData<Title>> = _searchQuery
        .debounce(300L)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            repository.getTitles(query)
        }
        .cachedIn(viewModelScope)

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun downloadCatalogueForOffline() {
        if (_syncState.value is SyncUiState.Syncing) return
        viewModelScope.launch {
            _syncState.value = SyncUiState.Syncing(progress = 0f, message = "Downloading catalogue…")
            val result = repository.syncAllCatalogue { page, totalPages ->
                _syncState.value = SyncUiState.Syncing(
                    progress = page.toFloat() / totalPages.toFloat(),
                    message = "Downloaded page $page of $totalPages"
                )
            }
            if (result.isSuccess) {
                _syncState.value = SyncUiState.Success(result.getOrDefault(0))
            } else {
                _syncState.value = SyncUiState.Error(
                    result.exceptionOrNull()?.localizedMessage ?: "Download failed"
                )
            }
        }
    }

    fun dismissSyncBanner() {
        _syncState.value = SyncUiState.Idle
    }
}

sealed interface SyncUiState {
    data object Idle : SyncUiState
    data class Syncing(val progress: Float, val message: String) : SyncUiState
    data class Success(val totalDownloaded: Int) : SyncUiState
    data class Error(val message: String) : SyncUiState
}
