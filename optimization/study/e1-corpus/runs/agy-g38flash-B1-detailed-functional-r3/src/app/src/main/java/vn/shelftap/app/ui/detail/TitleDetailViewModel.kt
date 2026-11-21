package vn.shelftap.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.shelftap.app.data.model.Title
import vn.shelftap.app.data.repository.CatalogueRepository

sealed interface TitleDetailUiState {
    object Loading : TitleDetailUiState
    data class Success(val title: Title) : TitleDetailUiState
    data class Error(val message: String) : TitleDetailUiState
}

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
            repository.getTitleByIdFlow(titleId).collect { cached ->
                if (cached != null) {
                    _uiState.value = TitleDetailUiState.Success(cached)
                } else {
                    // Try remote fetch if not found in cache
                    val remote = repository.getTitleById(titleId)
                    if (remote != null) {
                        _uiState.value = TitleDetailUiState.Success(remote)
                    } else {
                        _uiState.value = TitleDetailUiState.Error("Title not found")
                    }
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            titleId: String,
            repository: CatalogueRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TitleDetailViewModel(titleId, repository) as T
            }
        }
    }
}
