package vn.shelftap.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.model.HistoryItem
import vn.shelftap.app.data.repository.ShelfTapRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HistoryViewModel(
    private val repository: ShelfTapRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState(isLoading = true))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Fetch profile for historyEnabled state
            val profileResult = repository.getProfile()
            val historyEnabled = profileResult.getOrNull()?.profile?.historyEnabled ?: true

            // Fetch history
            val historyResult = repository.getHistory()
            historyResult.fold(
                onSuccess = { historyData ->
                    val uiItems = historyData.history
                        .map { mapToUiItem(it) }
                        .sortedByDescending { it.sortInstantEpoch }

                    _uiState.update {
                        it.copy(
                            history = uiItems,
                            historyEnabled = historyEnabled,
                            isSavedData = historyData.isSavedData,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            historyEnabled = historyEnabled,
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to load history"
                        )
                    }
                }
            )
        }
    }

    fun onHistorySwitchToggled(checked: Boolean) {
        if (!checked) {
            // Turning it off asks for confirmation first
            _uiState.update { it.copy(showDisableConfirmDialog = true) }
        } else {
            // Turning it on directly
            updatePrivacySetting(enabled = true)
        }
    }

    fun confirmDisableHistory() {
        _uiState.update { it.copy(showDisableConfirmDialog = false) }
        updatePrivacySetting(enabled = false)
    }

    fun dismissDisableConfirmDialog() {
        _uiState.update { it.copy(showDisableConfirmDialog = false) }
    }

    private fun updatePrivacySetting(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.updatePrivacy(enabled)
            result.fold(
                onSuccess = { profile ->
                    _uiState.update {
                        it.copy(
                            historyEnabled = profile.historyEnabled,
                            isLoading = false
                        )
                    }
                    // Refresh history list (spec: "empty list when the member has switched history off")
                    loadData()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to update history setting"
                        )
                    }
                }
            )
        }
    }

    private fun mapToUiItem(item: HistoryItem): HistoryUiItem {
        val zoneId = ZoneId.systemDefault()
        var formattedBorrowed = item.outAt
        var formattedReturned = item.returnedAt ?: "Not returned"
        var sortEpoch = 0L

        try {
            val outInstant = Instant.parse(item.outAt)
            formattedBorrowed = dateFormatter.format(outInstant.atZone(zoneId))
            sortEpoch = outInstant.epochSecond

            if (item.returnedAt != null) {
                val retInstant = Instant.parse(item.returnedAt)
                formattedReturned = dateFormatter.format(retInstant.atZone(zoneId))
                sortEpoch = retInstant.epochSecond
            }
        } catch (e: Exception) {
            // fallback
        }

        return HistoryUiItem(
            titleId = item.titleId,
            title = item.title,
            formattedBorrowedDate = formattedBorrowed,
            formattedReturnedDate = formattedReturned,
            sortInstantEpoch = sortEpoch
        )
    }
}
