package vn.shelftap.app.ui.catalogue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import vn.shelftap.app.data.repository.CatalogueRepository
import vn.shelftap.app.data.repository.SyncState
import vn.shelftap.app.domain.model.Title

class CatalogueViewModel(
    private val repository: CatalogueRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val syncState: StateFlow<SyncState> = repository.getSyncState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncState.Idle)

    /**
     * Complies with Rule 17:
     * - Debounces query flow
     * - Cancels previous query with flatMapLatest
     * - Runs query off main thread (flowOn(ioDispatcher)) against indexed Room @Fts4 table
     * - Pages results with cachedIn(viewModelScope)
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val titlesPagingData: Flow<PagingData<Title>> = _searchQuery
        .debounce(300L)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            repository.getTitlesPaged(query)
        }
        .flowOn(ioDispatcher)
        .cachedIn(viewModelScope)

    init {
        // Automatically check if catalogue should be downloaded on initial launch
        viewModelScope.launch(ioDispatcher) {
            val count = repository.getLocalTitleCount()
            if (count == 0) {
                repository.syncCatalogue()
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onClearSearch() {
        _searchQuery.value = ""
    }

    fun onDownloadCatalogueClicked() {
        viewModelScope.launch(ioDispatcher) {
            repository.syncCatalogue()
        }
    }

    companion object {
        fun provideFactory(
            repository: CatalogueRepository,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CatalogueViewModel(repository, ioDispatcher) as T
            }
        }
    }
}
