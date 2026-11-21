package vn.shelftap.app.ui

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
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.SyncStatus
import vn.shelftap.app.data.TitleEntity

class CatalogueViewModel(repository: CatalogueRepository) : ViewModel() {
    val titles: Flow<PagingData<TitleEntity>> = repository.pagingAll().cachedIn(viewModelScope)
    val sync: StateFlow<SyncStatus> = repository.sync
}

class SearchViewModel(repository: CatalogueRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val results: Flow<PagingData<TitleEntity>> = _query
        .debounce(150L)
        .distinctUntilChanged()
        .flatMapLatest { repository.pagingSearch(it) }
        .cachedIn(viewModelScope)

    fun onQueryChange(text: String) {
        _query.value = text
    }
}

sealed interface DetailState {
    data object Loading : DetailState
    data object Missing : DetailState
    data class Found(val title: TitleEntity) : DetailState
}

class DetailViewModel(
    private val repository: CatalogueRepository,
    private val titleId: String,
) : ViewModel() {
    private val _state = MutableStateFlow<DetailState>(DetailState.Loading)
    val state: StateFlow<DetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = repository.title(titleId)?.let { DetailState.Found(it) } ?: DetailState.Missing
        }
    }
}
