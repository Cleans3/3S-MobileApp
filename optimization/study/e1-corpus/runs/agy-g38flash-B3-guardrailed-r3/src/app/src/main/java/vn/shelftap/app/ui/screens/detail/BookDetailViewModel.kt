package vn.shelftap.app.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.model.TitleDetail
import vn.shelftap.app.data.repository.ShelfTapRepository

data class BookDetailUiState(
    val isLoading: Boolean = true,
    val book: TitleDetail? = null,
    val errorMessage: String? = null
)

class BookDetailViewModel(
    private val repository: ShelfTapRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookDetailUiState())
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    fun loadTitleDetail(titleId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.getTitleDetail(titleId)
            result.fold(
                onSuccess = { detail ->
                    _uiState.update {
                        it.copy(isLoading = false, book = detail, errorMessage = null)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to load book details"
                        )
                    }
                }
            )
        }
    }

    companion object {
        fun provideFactory(repository: ShelfTapRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BookDetailViewModel(repository) as T
                }
            }
    }
}
