package vn.shelftap.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.shelftap.app.data.model.HistoryItem
import vn.shelftap.app.data.repository.ShelfTapRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class HistoryItemUiState(
    val item: HistoryItem,
    val formattedBorrowedDate: String,
    val formattedReturnedDate: String
)

data class HistoryUiState(
    val isLoading: Boolean = true,
    val historyItems: List<HistoryItemUiState> = emptyList(),
    val historyEnabled: Boolean = true,
    val showTurnOffConfirmationDialog: Boolean = false,
    val isUpdatingPrivacy: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)

class HistoryViewModel(
    private val repository: ShelfTapRepository,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Load profile for historyEnabled state
            val profileResult = repository.getProfile()
            var historyEnabled = true
            profileResult.onSuccess { dataResult ->
                historyEnabled = dataResult.data.historyEnabled
            }

            // Load history list
            val historyResult = repository.getHistory()
            historyResult.fold(
                onSuccess = { dataResult ->
                    val uiItems = withContext(defaultDispatcher) {
                        dataResult.data
                            .sortedByDescending { it.outAt } // newest first
                            .map { mapToUiModel(it) }
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            historyItems = uiItems,
                            historyEnabled = historyEnabled,
                            isOffline = dataResult.isFromCache,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            historyEnabled = historyEnabled,
                            errorMessage = error.localizedMessage ?: "Failed to load history"
                        )
                    }
                }
            )
        }
    }

    fun onPrivacySwitchToggled(enabled: Boolean) {
        if (!enabled) {
            // Turning it off requires confirmation first
            _uiState.update { it.copy(showTurnOffConfirmationDialog = true) }
        } else {
            // Turning it on directly
            applyPrivacyChange(true)
        }
    }

    fun dismissConfirmDialog() {
        _uiState.update { it.copy(showTurnOffConfirmationDialog = false) }
    }

    fun confirmTurnOffHistory() {
        _uiState.update { it.copy(showTurnOffConfirmationDialog = false) }
        applyPrivacyChange(false)
    }

    private fun applyPrivacyChange(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingPrivacy = true) }
            val result = repository.updateHistoryPrivacy(enabled)
            result.fold(
                onSuccess = { updatedProfile ->
                    _uiState.update {
                        it.copy(
                            isUpdatingPrivacy = false,
                            historyEnabled = updatedProfile.historyEnabled,
                            historyItems = if (!updatedProfile.historyEnabled) emptyList() else it.historyItems
                        )
                    }
                    if (updatedProfile.historyEnabled) {
                        // Reload history if switched on
                        loadData()
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isUpdatingPrivacy = false,
                            errorMessage = error.localizedMessage ?: "Failed to update privacy"
                        )
                    }
                }
            )
        }
    }

    private fun mapToUiModel(item: HistoryItem): HistoryItemUiState {
        val borrowed = try {
            val instant = Instant.parse(item.outAt)
            val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
            dateFormatter.format(date)
        } catch (_: Exception) {
            item.outAt
        }

        val returned = if (item.returnedAt != null) {
            try {
                val instant = Instant.parse(item.returnedAt)
                val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                dateFormatter.format(date)
            } catch (_: Exception) {
                item.returnedAt
            }
        } else {
            ""
        }

        return HistoryItemUiState(
            item = item,
            formattedBorrowedDate = borrowed,
            formattedReturnedDate = returned
        )
    }

    companion object {
        fun provideFactory(repository: ShelfTapRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HistoryViewModel(repository) as T
                }
            }
    }
}
