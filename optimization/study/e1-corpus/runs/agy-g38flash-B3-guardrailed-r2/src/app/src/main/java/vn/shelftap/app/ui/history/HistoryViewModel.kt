package vn.shelftap.app.ui.history

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
import vn.shelftap.app.data.repository.HistoryRepository
import vn.shelftap.app.data.repository.HistoryResult
import vn.shelftap.app.data.repository.UserProfileResult
import vn.shelftap.app.data.repository.UserRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HistoryViewModel(
    private val historyRepository: HistoryRepository,
    private val userRepository: UserRepository,
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState(isLoading = true))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Load profile for historyEnabled flag
            var enabled = _uiState.value.historyEnabled
            var savedData = false
            when (val userResult = userRepository.getUserProfile()) {
                is UserProfileResult.Success -> {
                    enabled = userResult.profile.historyEnabled
                    savedData = userResult.isSavedData
                }
                is UserProfileResult.Error -> {}
            }

            // Load history items
            when (val historyResult = historyRepository.getHistory()) {
                is HistoryResult.Success -> {
                    val mappedItems = withContext(computationDispatcher) {
                        // Sort newest first by outAt
                        historyResult.history
                            .sortedByDescending { it.outAt }
                            .mapIndexed { index, item -> mapToUi(index, item) }
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            historyItems = mappedItems,
                            historyEnabled = enabled,
                            isSavedData = historyResult.isSavedData || savedData,
                            error = null
                        )
                    }
                }
                is HistoryResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            historyEnabled = enabled,
                            error = historyResult.message
                        )
                    }
                }
            }
        }
    }

    fun onHistorySwitchToggled(targetState: Boolean) {
        if (!targetState) {
            // Turning it off requires confirmation first
            _uiState.update { it.copy(showConfirmDisableDialog = true) }
        } else {
            // Turning it on does not require confirmation
            updateHistoryEnabled(true)
        }
    }

    fun onConfirmDisableHistory() {
        _uiState.update { it.copy(showConfirmDisableDialog = false) }
        updateHistoryEnabled(false)
    }

    fun onDismissDisableDialog() {
        _uiState.update { it.copy(showConfirmDisableDialog = false) }
    }

    private fun updateHistoryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingPrivacy = true) }
            val result = userRepository.updateHistoryEnabled(enabled)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        historyEnabled = enabled,
                        isUpdatingPrivacy = false
                    )
                }
                // Refresh history as per server behavior
                loadData()
            } else {
                _uiState.update {
                    it.copy(
                        isUpdatingPrivacy = false,
                        error = "Failed to update privacy setting"
                    )
                }
            }
        }
    }

    private fun mapToUi(index: Int, item: HistoryItem): HistoryItemUi {
        val borrowedFormatted = try {
            val instant = Instant.parse(item.outAt)
            instant.atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
        } catch (e: Exception) {
            item.outAt
        }

        val returnedFormatted = if (item.returnedAt != null) {
            try {
                val instant = Instant.parse(item.returnedAt)
                instant.atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
            } catch (e: Exception) {
                item.returnedAt
            }
        } else {
            "Still checked out"
        }

        return HistoryItemUi(
            id = "${item.titleId}_${item.outAt}_$index",
            titleId = item.titleId,
            title = item.title,
            borrowedDateFormatted = borrowedFormatted,
            returnedDateFormatted = returnedFormatted
        )
    }

    companion object {
        fun provideFactory(
            historyRepository: HistoryRepository,
            userRepository: UserRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HistoryViewModel(historyRepository, userRepository) as T
                }
            }
    }
}
