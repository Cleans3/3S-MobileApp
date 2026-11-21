package vn.shelftap.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.shelftap.app.data.repository.CatalogueRepository

/**
 * ViewModel responsible for loading detailed book information by title ID.
 */
class TitleDetailViewModel(
    private val titleId: String,
    private val repository: CatalogueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TitleDetailUiState>(TitleDetailUiState.Loading)
    val uiState: StateFlow<TitleDetailUiState> = _uiState.asStateFlow()

    init {
        loadTitle()
    }

    fun loadTitle() {
        viewModelScope.launch {
            _uiState.value = TitleDetailUiState.Loading
            val title = repository.getTitleById(titleId)
            if (title != null) {
                _uiState.value = TitleDetailUiState.Success(title)
            } else {
                _uiState.value = TitleDetailUiState.Error("Title not found in catalogue")
            }
        }
    }

    class Factory(
        private val titleId: String,
        private val repository: CatalogueRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TitleDetailViewModel(titleId, repository) as T
        }
    }
}
