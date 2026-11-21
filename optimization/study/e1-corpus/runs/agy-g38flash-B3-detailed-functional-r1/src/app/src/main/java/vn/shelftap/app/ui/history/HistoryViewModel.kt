package vn.shelftap.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.model.HistoryItem
import vn.shelftap.app.data.repository.LibraryRepository
import vn.shelftap.app.data.repository.Resource

data class HistoryUiState(
    val historyItems: List<HistoryItem> = emptyList(),
    val historyEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val isUpdatingPrivacy: Boolean = false,
    val showTurnOffConfirmDialog: Boolean = false,
    val userMessage: String? = null
)

class HistoryViewModel(
    private val repository: LibraryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState(isLoading = true))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Fetch profile for historyEnabled
            val meResult = repository.getMe()
            if (meResult.isSuccess) {
                val me = meResult.getOrNull()
                if (me != null) {
                    _uiState.update { it.copy(historyEnabled = me.historyEnabled) }
                }
            }

            // Fetch history items
            when (val res = repository.getHistory()) {
                is Resource.Success -> {
                    // Sort newest first by outAt descending
                    val sorted = res.data.sortedByDescending { it.outAt }
                    _uiState.update {
                        it.copy(
                            historyItems = sorted,
                            isOffline = res.isOffline,
                            isLoading = false
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userMessage = res.message
                        )
                    }
                }
            }
        }
    }

    fun onHistorySwitchToggled(enabled: Boolean) {
        if (!enabled) {
            // Turning OFF requires user confirmation first
            _uiState.update { it.copy(showTurnOffConfirmDialog = true) }
        } else {
            // Turning ON applies immediately
            updatePrivacy(true)
        }
    }

    fun confirmTurnOffHistory() {
        _uiState.update { it.copy(showTurnOffConfirmDialog = false) }
        updatePrivacy(false)
    }

    fun dismissConfirmDialog() {
        _uiState.update { it.copy(showTurnOffConfirmDialog = false) }
    }

    private fun updatePrivacy(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingPrivacy = true) }
            val result = repository.updatePrivacy(enabled)
            if (result.isSuccess) {
                val updatedMe = result.getOrNull()
                _uiState.update {
                    it.copy(
                        historyEnabled = updatedMe?.historyEnabled ?: enabled,
                        isUpdatingPrivacy = false
                    )
                }
                // Refresh history list
                loadData()
            } else {
                _uiState.update {
                    it.copy(
                        isUpdatingPrivacy = false,
                        userMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to update setting"
                    )
                }
            }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    companion object {
        fun provideFactory(repository: LibraryRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HistoryViewModel(repository) as T
                }
            }
    }
}
