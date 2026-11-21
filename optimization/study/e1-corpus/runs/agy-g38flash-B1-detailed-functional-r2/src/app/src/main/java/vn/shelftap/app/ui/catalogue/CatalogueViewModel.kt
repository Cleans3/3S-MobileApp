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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import vn.shelftap.app.data.repository.CatalogueRepository
import vn.shelftap.app.data.sync.SyncStatus
import vn.shelftap.app.model.Title

class CatalogueViewModel(
    private val repository: CatalogueRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val totalCachedCount: StateFlow<Int> = repository.getLocalTitleCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val syncStatus: StateFlow<SyncStatus> = repository.syncStatus

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val titles: Flow<PagingData<Title>> = _searchQuery
        .debounce { query -> if (query.isEmpty()) 0L else 200L }
        .flatMapLatest { query ->
            repository.getTitles(query)
        }
        .cachedIn(viewModelScope)

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun syncCatalogue() {
        viewModelScope.launch {
            repository.syncEntireCatalogue()
        }
    }

    companion object {
        fun provideFactory(repository: CatalogueRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CatalogueViewModel(repository) as T
                }
            }
    }
}
