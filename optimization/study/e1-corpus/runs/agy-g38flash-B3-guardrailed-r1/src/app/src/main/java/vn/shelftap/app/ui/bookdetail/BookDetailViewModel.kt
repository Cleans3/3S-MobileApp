package vn.shelftap.app.ui.bookdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.model.BookDetail
import vn.shelftap.app.data.repository.LibraryRepository

data class BookDetailUiState(
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val book: BookDetail? = null,
    val error: String? = null
)

class BookDetailViewModel(
    private val titleId: String,
    private val repository: LibraryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookDetailUiState(isLoading = true))
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    init {
        loadBookDetail()
    }

    fun loadBookDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val cachedResult = repository.getTitleDetail(titleId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isOffline = cachedResult.isOffline,
                        book = cachedResult.data,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load book details"
                    )
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            titleId: String,
            repository: LibraryRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BookDetailViewModel(titleId, repository) as T
                }
            }
    }
}
