package vn.shelftap.app.ui.bookdetail

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
import vn.shelftap.app.util.Resource

data class BookDetailUiState(
    val isLoading: Boolean = true,
    val titleDetail: TitleDetail? = null,
    val isSavedData: Boolean = false,
    val errorMessage: String? = null
)

class BookDetailViewModel(
    private val titleId: String,
    private val repository: ShelfTapRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookDetailUiState())
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    init {
        loadTitleDetail()
    }

    fun loadTitleDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getTitleDetail(titleId)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            titleDetail = result.data,
                            isSavedData = result.isSavedData,
                            errorMessage = null
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSavedData = result.isSavedData,
                            errorMessage = result.message
                        )
                    }
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    class Factory(
        private val titleId: String,
        private val repository: ShelfTapRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BookDetailViewModel::class.java)) {
                return BookDetailViewModel(titleId, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
