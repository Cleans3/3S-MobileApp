package vn.shelftap.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.repository.TitleRepository

class BookDetailViewModel(
    private val titleId: String,
    private val titleRepository: TitleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookDetailUiState(isLoading = true))
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    init {
        loadTitleDetail()
    }

    fun loadTitleDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = titleRepository.getTitleDetail(titleId)
            result.fold(
                onSuccess = { (detail, isSaved) ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            titleDetail = detail,
                            isSavedData = isSaved,
                            errorMessage = null
                        )
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
        fun provideFactory(
            titleId: String,
            titleRepository: TitleRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BookDetailViewModel(titleId, titleRepository) as T
            }
        }
    }
}
