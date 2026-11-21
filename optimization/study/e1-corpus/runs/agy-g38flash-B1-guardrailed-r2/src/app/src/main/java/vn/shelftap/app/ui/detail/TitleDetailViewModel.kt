package vn.shelftap.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.shelftap.app.data.repository.CatalogueRepository
import vn.shelftap.app.domain.model.Title
import vn.shelftap.app.util.DispatcherProvider

sealed interface TitleDetailUiState {
    data object Loading : TitleDetailUiState
    data class Success(val title: Title) : TitleDetailUiState
    data object NotFound : TitleDetailUiState
}

class TitleDetailViewModel(
    private val titleId: String,
    private val repository: CatalogueRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<TitleDetailUiState>(TitleDetailUiState.Loading)
    val uiState: StateFlow<TitleDetailUiState> = _uiState.asStateFlow()

    init {
        loadTitle()
    }

    private fun loadTitle() {
        viewModelScope.launch(dispatchers.io) {
            val title = repository.getTitleById(titleId)
            if (title != null) {
                _uiState.value = TitleDetailUiState.Success(title)
            } else {
                _uiState.value = TitleDetailUiState.NotFound
            }

            // Also observe any updates from local database
            repository.observeTitleById(titleId).collect { updated ->
                if (updated != null) {
                    _uiState.value = TitleDetailUiState.Success(updated)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch(dispatchers.io) {
            repository.refreshTitle(titleId)
        }
    }

    class Factory(
        private val titleId: String,
        private val repository: CatalogueRepository,
        private val dispatchers: DispatcherProvider
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TitleDetailViewModel(titleId, repository, dispatchers) as T
        }
    }
}
