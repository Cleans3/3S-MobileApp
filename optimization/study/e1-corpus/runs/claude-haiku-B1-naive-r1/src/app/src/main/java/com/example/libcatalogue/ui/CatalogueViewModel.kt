package com.example.libcatalogue.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.libcatalogue.LibraryApp
import com.example.libcatalogue.data.Book
import com.example.libcatalogue.data.BookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class CatalogueViewModel(repository: BookRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /**
     * Paged results for the current search. Typing is debounced so a request is only
     * sent once the member pauses; clearing the search is applied immediately.
     */
    val books: Flow<PagingData<Book>> = _query
        .debounce { text -> if (text.isEmpty()) 0L else SEARCH_DEBOUNCE_MS }
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { repository.books(it) }
        .cachedIn(viewModelScope)

    fun onQueryChange(text: String) {
        _query.value = text
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LibraryApp
                CatalogueViewModel(app.container.bookRepository)
            }
        }
    }
}
