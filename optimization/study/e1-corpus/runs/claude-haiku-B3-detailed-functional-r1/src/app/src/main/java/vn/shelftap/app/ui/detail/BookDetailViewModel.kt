package vn.shelftap.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.BookTitle
import vn.shelftap.app.data.LibraryRepository
import vn.shelftap.app.ui.toUserMessage

data class BookDetailUiState(
    val book: BookTitle? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

class BookDetailViewModel(
    private val titleId: String,
    private val library: LibraryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(BookDetailUiState())
    val state: StateFlow<BookDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val book = library.title(titleId)
                _state.update { it.copy(book = book, loading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "Could not load this book: ${e.toUserMessage()}") }
            }
        }
    }
}
