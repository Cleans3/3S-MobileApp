package vn.shelftap.app.ui.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.ShelfRepository
import vn.shelftap.app.data.Title
import vn.shelftap.app.data.userMessage

data class BookUiState(
    val loading: Boolean = true,
    val title: Title? = null,
    val error: String? = null,
)

class BookViewModel(
    private val repo: ShelfRepository,
    private val titleId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(BookUiState())
    val state: StateFlow<BookUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val title = repo.title(titleId)
                _state.update { it.copy(loading = false, title = title) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }
}
