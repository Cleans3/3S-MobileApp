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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import vn.shelftap.app.data.repository.CatalogueRepository
import vn.shelftap.app.domain.model.Title
import vn.shelftap.app.util.DispatcherProvider

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class CatalogueViewModel(
    private val repository: CatalogueRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Rule 17: Debounce query flow, cancel previous query on new arrival (flatMapLatest),
    // run off main thread (flowOn), query Room FTS4, and page the results
    val titles: Flow<PagingData<Title>> = _searchQuery
        .debounce(300L)
        .flatMapLatest { query ->
            repository.getTitles(query)
        }
        .flowOn(dispatchers.io)
        .cachedIn(viewModelScope)

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    class Factory(
        private val repository: CatalogueRepository,
        private val dispatchers: DispatcherProvider
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CatalogueViewModel(repository, dispatchers) as T
        }
    }
}
