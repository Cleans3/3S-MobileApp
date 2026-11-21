package vn.shelftap.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.shelftap.app.data.model.Title
import vn.shelftap.app.data.repository.CatalogueRepository

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Success(val title: Title) : DetailUiState
    data object NotFound : DetailUiState
}

class TitleDetailViewModel(
    private val repository: CatalogueRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadTitle(titleId: String) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.value = DetailUiState.Loading
            val title = repository.getTitleById(titleId)
            _uiState.value = if (title != null) {
                DetailUiState.Success(title)
            } else {
                DetailUiState.NotFound
            }
        }
    }
}
