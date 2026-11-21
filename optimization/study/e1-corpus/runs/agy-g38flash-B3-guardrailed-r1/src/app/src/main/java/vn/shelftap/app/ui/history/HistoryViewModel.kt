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
import vn.shelftap.app.util.DateFormatterUtil
import java.time.Instant

data class HistoryItemUiState(
    val item: HistoryItem,
    val formattedBorrowedDate: String,
    val formattedReturnedDate: String,
    val sortInstant: Instant
)

data class HistoryUiState(
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val historyEnabled: Boolean = true,
    val isUpdatingHistoryToggle: Boolean = false,
    val historyItems: List<HistoryItemUiState> = emptyList(),
    val showConfirmDisableDialog: Boolean = false,
    val error: String? = null
)

class HistoryViewModel(
    private val repository: LibraryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState(isLoading = true))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Fetch profile to know historyEnabled status
                val profileResult = repository.getProfile()
                val isEnabled = profileResult.data.historyEnabled

                // Fetch history
                val historyResult = repository.getHistory()

                // Rule 14: Pre-format and sort newest first in ViewModel
                val formattedItems = historyResult.data.map { item ->
                    val sortInstant = DateFormatterUtil.parseInstant(item.returnedAt)
                        ?: DateFormatterUtil.parseInstant(item.outAt)
                        ?: Instant.EPOCH

                    val returnedText = if (item.returnedAt != null) {
                        DateFormatterUtil.formatDate(item.returnedAt)
                    } else {
                        "Currently out"
                    }

                    HistoryItemUiState(
                        item = item,
                        formattedBorrowedDate = DateFormatterUtil.formatDate(item.outAt),
                        formattedReturnedDate = returnedText,
                        sortInstant = sortInstant
                    )
                }.sortedByDescending { it.sortInstant }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isOffline = historyResult.isOffline || profileResult.isOffline,
                        historyEnabled = isEnabled,
                        historyItems = formattedItems,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load history"
                    )
                }
            }
        }
    }

    fun onSwitchToggled(newChecked: Boolean) {
        if (!newChecked) {
            // Turning off requires confirmation first
            _uiState.update { it.copy(showConfirmDisableDialog = true) }
        } else {
            // Turning on can be applied directly
            updatePrivacy(true)
        }
    }

    fun onConfirmDisableHistory() {
        _uiState.update { it.copy(showConfirmDisableDialog = false) }
        updatePrivacy(false)
    }

    fun onDismissConfirmDialog() {
        _uiState.update { it.copy(showConfirmDisableDialog = false) }
    }

    private fun updatePrivacy(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingHistoryToggle = true) }
            try {
                val updatedProfile = repository.setHistoryEnabled(enabled)
                _uiState.update {
                    it.copy(
                        historyEnabled = updatedProfile.historyEnabled,
                        isUpdatingHistoryToggle = false,
                        historyItems = if (!updatedProfile.historyEnabled) emptyList() else it.historyItems
                    )
                }
                if (enabled) {
                    loadHistory()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUpdatingHistoryToggle = false,
                        error = e.message ?: "Failed to update history setting"
                    )
                }
            }
        }
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
