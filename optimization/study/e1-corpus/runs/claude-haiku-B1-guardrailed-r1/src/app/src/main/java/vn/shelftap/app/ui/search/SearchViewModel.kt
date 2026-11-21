package vn.shelftap.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.buildSearchMatch
import vn.shelftap.app.domain.Title

private const val SEARCH_DEBOUNCE_MS = 300L

class SearchViewModel(private val repository: CatalogueRepository) : ViewModel() {

    private val queryState = MutableStateFlow("")

    /** The text currently in the search field (not debounced, so typing feels immediate). */
    val query: StateFlow<String> = queryState.asStateFlow()

    /**
     * Results for the debounced query. Each new query cancels the previous one (flatMapLatest),
     * and the matching is done by the database off the main thread.
     */
    @OptIn(FlowPreview::class)
    val results: Flow<PagingData<Title>> = queryState
        .debounce(SEARCH_DEBOUNCE_MS)
        .map { buildSearchMatch(it) }
        .distinctUntilChanged()
        .flatMapLatest { match ->
            if (match == null) {
                flowOf(PagingData.empty<Title>())
            } else {
                repository.search(match)
            }
        }
        .cachedIn(viewModelScope)

    fun onQueryChange(text: String) {
        queryState.value = text
    }

    companion object {
        fun factory(repository: CatalogueRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { SearchViewModel(repository) }
            }
    }
}
