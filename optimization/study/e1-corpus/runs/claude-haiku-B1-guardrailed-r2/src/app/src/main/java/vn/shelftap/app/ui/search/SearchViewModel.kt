package vn.shelftap.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.FtsQuery
import vn.shelftap.app.data.TitleEntity

class SearchViewModel(private val repository: CatalogueRepository) : ViewModel() {

    private val queryText = MutableStateFlow("")

    /** The text field shows this immediately; only the result list waits for the debounce. */
    val query: StateFlow<String> = queryText.asStateFlow()

    /**
     * Results for the latest query. Each keystroke restarts the debounce, and the previous
     * query is cancelled by flatMapLatest. Empty input shows the whole catalogue.
     */
    val results: Flow<PagingData<TitleEntity>> = queryText
        .debounce(SEARCH_DEBOUNCE_MS)
        .map { FtsQuery.build(it) }
        .distinctUntilChanged()
        .flatMapLatest { match ->
            Pager(
                config = PagingConfig(
                    pageSize = CatalogueRepository.PAGE_SIZE,
                    enablePlaceholders = false
                ),
                pagingSourceFactory = {
                    if (match == null) repository.pagingAll() else repository.pagingSearch(match)
                }
            ).flow
        }
        .cachedIn(viewModelScope)

    fun onQueryChange(text: String) {
        queryText.value = text
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
