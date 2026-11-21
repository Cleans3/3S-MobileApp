package vn.shelftap.app.ui.history

data class HistoryItemUi(
    val id: String,
    val titleId: String,
    val title: String,
    val borrowedDateFormatted: String,
    val returnedDateFormatted: String
)

data class HistoryUiState(
    val isLoading: Boolean = false,
    val historyItems: List<HistoryItemUi> = emptyList(),
    val historyEnabled: Boolean = true,
    val isSavedData: Boolean = false,
    val showConfirmDisableDialog: Boolean = false,
    val isUpdatingPrivacy: Boolean = false,
    val error: String? = null
)
