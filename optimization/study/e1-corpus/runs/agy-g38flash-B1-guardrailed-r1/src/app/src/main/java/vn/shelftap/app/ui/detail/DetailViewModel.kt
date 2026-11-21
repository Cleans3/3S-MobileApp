package vn.shelftap.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.shelftap.app.data.model.Title
import vn.shelftap.app.data.repository.CatalogueRepository

class DetailViewModel(
    private val titleId: String,
    private val repository: CatalogueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadTitle()
    }

    private fun loadTitle() {
        viewModelScope.launch {
            repository.getTitleById(titleId).collect { title ->
                if (title != null) {
                    _uiState.value = DetailUiState.Success(title)
                } else {
                    val result = repository.refreshTitle(titleId)
                    if (result.isSuccess) {
                        _uiState.value = DetailUiState.Success(result.getOrThrow())
                    } else if (_uiState.value !is DetailUiState.Success) {
                        _uiState.value = DetailUiState.Error(
                            result.exceptionOrNull()?.localizedMessage ?: "Title not found"
                        )
                    }
                }
            }
        }
    }

    fun retry() {
        _uiState.value = DetailUiState.Loading
        loadTitle()
    }
}

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Success(val title: Title) : DetailUiState
    data class Error(val message: String) : DetailUiState
}
