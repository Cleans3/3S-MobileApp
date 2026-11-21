package com.example.libcatalog.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.libcatalog.data.Book
import com.example.libcatalog.data.BookRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

private const val SEARCH_DEBOUNCE_MS = 300L

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class CatalogViewModel(repository: BookRepository) : ViewModel() {

    private val _query = MutableStateFlow("")

    /** The text currently in the search box. */
    val query: StateFlow<String> = _query.asStateFlow()

    /**
     * Paged book list for the current search. Typing is debounced so each keystroke
     * does not hit the server; an empty query (clearing the box) is applied immediately.
     */
    val books: Flow<PagingData<Book>> = _query
        .map { it.trim() }
        .debounce { if (it.isEmpty()) 0L else SEARCH_DEBOUNCE_MS }
        .distinctUntilChanged()
        .flatMapLatest { repository.search(it) }
        .cachedIn(viewModelScope)

    fun onQueryChange(text: String) {
        _query.value = text
    }
}

class CatalogViewModelFactory(private val repository: BookRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CatalogViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return CatalogViewModel(repository) as T
    }
}
