package com.example.librarycatalogue.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.librarycatalogue.LibraryApp
import com.example.librarycatalogue.data.repository.BookRepository
import com.example.librarycatalogue.model.Book
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class CatalogueViewModel(
    private val repository: BookRepository = LibraryApp.instance.bookRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGenre = MutableStateFlow("All Genres")
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    private val _selectedBook = MutableStateFlow<Book?>(null)
    val selectedBook: StateFlow<Book?> = _selectedBook.asStateFlow()

    val totalBooks: StateFlow<Int> = repository.totalBooksCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isSeeding: StateFlow<Boolean> = repository.isSeeding
    val seedingProgress: StateFlow<Float> = repository.seedingProgress

    // Debounced query combined with selected genre to prevent rapid DB queries
    private val debouncedQuery = _searchQuery
        .debounce(250L)

    val books: Flow<PagingData<Book>> = combine(
        debouncedQuery,
        _selectedGenre
    ) { query, genre ->
        Pair(query, genre)
    }.flatMapLatest { (query, genre) ->
        repository.getBooks(query, genre)
    }.cachedIn(viewModelScope)

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onGenreSelected(genre: String) {
        _selectedGenre.value = genre
    }

    fun onBookSelected(book: Book?) {
        _selectedBook.value = book
    }

    fun toggleAvailability(book: Book) {
        viewModelScope.launch {
            repository.toggleAvailability(book.id, book.isAvailable)
            _selectedBook.value = book.copy(isAvailable = !book.isAvailable)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CatalogueViewModel() as T
            }
        }
    }
}
