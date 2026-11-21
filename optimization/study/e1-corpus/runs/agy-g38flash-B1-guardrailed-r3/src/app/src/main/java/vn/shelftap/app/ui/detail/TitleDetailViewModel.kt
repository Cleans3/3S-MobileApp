package vn.shelftap.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.shelftap.app.data.repository.CatalogueRepository

class TitleDetailViewModel(
    private val titleId: String,
    private val repository: CatalogueRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow<TitleDetailUiState>(TitleDetailUiState.Loading)
    val uiState: StateFlow<TitleDetailUiState> = _uiState.asStateFlow()

    init {
        loadTitle()
    }

    private fun loadTitle() {
        viewModelScope.launch(ioDispatcher) {
            _uiState.value = TitleDetailUiState.Loading
            val title = repository.getTitleById(titleId)
            if (title != null) {
                _uiState.value = TitleDetailUiState.Success(title)
            } else {
                _uiState.value = TitleDetailUiState.Error
            }
        }
    }

    fun retry() {
        loadTitle()
    }

    companion object {
        fun provideFactory(
            titleId: String,
            repository: CatalogueRepository,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TitleDetailViewModel(titleId, repository, ioDispatcher) as T
            }
        }
    }
}
