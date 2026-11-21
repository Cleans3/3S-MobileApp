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
import vn.shelftap.app.data.repository.ShelfTapRepository
import vn.shelftap.app.util.DateTimeUtils
import vn.shelftap.app.util.Resource

data class HistoryUiState(
    val isLoading: Boolean = false,
    val historyItems: List<HistoryItem> = emptyList(),
    val historyEnabled: Boolean = true,
    val isSavedData: Boolean = false,
    val isUpdatingPrivacy: Boolean = false,
    val showConfirmDisableDialog: Boolean = false,
    val errorMessage: String? = null,
    val userNotice: String? = null
)

class HistoryViewModel(
    private val repository: ShelfTapRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState(isLoading = true))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Load profile for historyEnabled
            val profileRes = repository.getMemberProfile()
            if (profileRes is Resource.Success) {
                _uiState.update { it.copy(historyEnabled = profileRes.data.historyEnabled) }
            }

            // Load history items
            when (val historyRes = repository.getHistory()) {
                is Resource.Success -> {
                    // Sort newest first based on outAt
                    val sorted = historyRes.data.sortedByDescending {
                        DateTimeUtils.parseInstant(it.outAt)
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            historyItems = sorted,
                            isSavedData = historyRes.isSavedData || (profileRes is Resource.Success && profileRes.isSavedData),
                            errorMessage = null
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSavedData = historyRes.isSavedData,
                            errorMessage = historyRes.message
                        )
                    }
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun onSwitchToggled(checked: Boolean) {
        if (!checked) {
            // Turning it off requires confirmation first
            _uiState.update { it.copy(showConfirmDisableDialog = true) }
        } else {
            // Turning it on does not require confirmation
            updatePrivacySetting(true)
        }
    }

    fun confirmDisableHistory() {
        _uiState.update { it.copy(showConfirmDisableDialog = false) }
        updatePrivacySetting(false)
    }

    fun dismissConfirmDialog() {
        _uiState.update { it.copy(showConfirmDisableDialog = false) }
    }

    private fun updatePrivacySetting(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingPrivacy = true) }
            when (val result = repository.updatePrivacy(enabled)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            historyEnabled = result.data.historyEnabled,
                            isUpdatingPrivacy = false,
                            userNotice = if (enabled) "Reading history enabled" else "Reading history disabled"
                        )
                    }
                    // Refresh history list from server/cache
                    val historyRes = repository.getHistory()
                    if (historyRes is Resource.Success) {
                        val sorted = historyRes.data.sortedByDescending {
                            DateTimeUtils.parseInstant(it.outAt)
                        }
                        _uiState.update { it.copy(historyItems = sorted) }
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isUpdatingPrivacy = false,
                            errorMessage = result.message,
                            userNotice = result.message
                        )
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun clearUserNotice() {
        _uiState.update { it.copy(userNotice = null) }
    }

    class Factory(private val repository: ShelfTapRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
                return HistoryViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
