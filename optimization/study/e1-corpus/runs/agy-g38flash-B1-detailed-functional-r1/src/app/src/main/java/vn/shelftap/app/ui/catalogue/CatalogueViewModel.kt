package vn.shelftap.app.ui.catalogue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import vn.shelftap.app.data.model.Title
import vn.shelftap.app.data.repository.CatalogueRepository

/**
 * ViewModel managing the catalogue list, live search query, and offline syncing.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class CatalogueViewModel(
    private val repository: CatalogueRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    /**
     * Debounced search query triggers Room database query updates.
     */
    private val debouncedQuery = _searchQuery
        .debounce(150L)
        .distinctUntilChanged()

    /**
     * Paged titles stream updating reactively as user types.
     */
    val titlesPagingData: Flow<PagingData<Title>> = debouncedQuery
        .flatMapLatest { query ->
            repository.getTitlesStream(query)
        }
        .cachedIn(viewModelScope)

    /**
     * Consolidated UI state for sync and total catalogue count.
     */
    val uiState: StateFlow<CatalogueUiState> = combine(
        _searchQuery,
        repository.localTitleCount,
        repository.syncStatus
    ) { query, count, syncStatus ->
        CatalogueUiState(
            searchQuery = query,
            totalCachedCount = count,
            syncStatus = syncStatus
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CatalogueUiState()
    )

    init {
        // Pre-seed sample titles if local DB is empty
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun startSync() {
        repository.downloadCatalogue()
    }

    class Factory(private val repository: CatalogueRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CatalogueViewModel(repository) as T
        }
    }
}
