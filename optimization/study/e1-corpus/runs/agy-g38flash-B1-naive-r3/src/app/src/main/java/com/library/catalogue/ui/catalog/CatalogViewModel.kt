package com.library.catalogue.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.library.catalogue.data.model.Book
import com.library.catalogue.data.repository.BookRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class CatalogViewModel(
    private val repository: BookRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedGenre = MutableStateFlow("All")
    private val _onlyAvailable = MutableStateFlow(false)
    private val _selectedBook = MutableStateFlow<Book?>(null)

    val uiState: StateFlow<CatalogUiState> = combine(
        _searchQuery,
        _selectedGenre,
        _onlyAvailable,
        _selectedBook,
        repository.getTotalBookCount(),
        repository.getGenres(),
        repository.seedingProgress
    ) { query, genre, onlyAvail, selected, count, genresList, progress ->
        CatalogUiState(
            searchQuery = query,
            selectedGenre = genre,
            onlyAvailable = onlyAvail,
            totalCount = count,
            genres = listOf("All") + genresList,
            selectedBook = selected,
            seedingProgress = progress
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CatalogUiState()
    )

    // Debounced search query & filter combination for PagingData flow
    val booksFlow: Flow<PagingData<Book>> = combine(
        _searchQuery.debounce(250).distinctUntilChanged(),
        _selectedGenre,
        _onlyAvailable
    ) { query, genre, onlyAvail ->
        Triple(query, genre, onlyAvail)
    }.flatMapLatest { (query, genre, onlyAvail) ->
        repository.getBooks(query, genre, onlyAvail)
    }.cachedIn(viewModelScope)

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onGenreSelected(genre: String) {
        _selectedGenre.value = genre
    }

    fun onToggleOnlyAvailable() {
        _onlyAvailable.value = !_onlyAvailable.value
    }

    fun onBookSelected(book: Book?) {
        _selectedBook.value = book
    }

    companion object {
        fun provideFactory(repository: BookRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CatalogViewModel(repository) as T
                }
            }
    }
}
