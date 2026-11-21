package vn.shelftap.app.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import vn.shelftap.app.ShelfTapApp
import vn.shelftap.app.model.Title

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as ShelfTapApp).container.repository

    private val _query = MutableStateFlow("")
    /** Text shown in the field. Updates on every keystroke. */
    val query: StateFlow<String> = _query.asStateFlow()

    /**
     * Results for the query. Waits for typing to pause, then cancels the previous query
     * (flatMapLatest) so only the latest search runs against the local index.
     */
    val results: Flow<PagingData<Title>> = _query
        .debounce(SEARCH_DEBOUNCE_MS)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { text -> repository.search(text) }
        .cachedIn(viewModelScope)

    fun onQueryChange(text: String) {
        _query.value = text
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L
    }
}
