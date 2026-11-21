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

class TitleDetailViewModel(
    private val titleId: String,
    private val titleRepository: TitleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TitleDetailUiState(isLoading = true))
    val uiState: StateFlow<TitleDetailUiState> = _uiState.asStateFlow()

    init {
        loadTitleDetails()
    }

    fun loadTitleDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = titleRepository.getTitleDetails(titleId)
            result.fold(
                onSuccess = { details ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            details = details,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.localizedMessage ?: "Failed to load book details"
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
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TitleDetailViewModel(titleId, titleRepository) as T
                }
            }
    }
}
