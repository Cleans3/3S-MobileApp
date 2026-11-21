package vn.shelftap.app.ui.history

import vn.shelftap.app.data.model.HistoryItem

data class HistoryUiState(
    val isLoading: Boolean = false,
    val historyItems: List<HistoryItem> = emptyList(),
    val isSavedData: Boolean = false,
    val historyEnabled: Boolean = true,
    val isUpdatingPrivacy: Boolean = false,
    val showTurnOffConfirmation: Boolean = false,
    val errorMessage: String? = null
)
