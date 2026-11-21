package vn.shelftap.app.ui.history

data class HistoryUiItem(
    val titleId: String,
    val title: String,
    val formattedBorrowedDate: String,
    val formattedReturnedDate: String,
    val sortInstantEpoch: Long
)

data class HistoryUiState(
    val history: List<HistoryUiItem> = emptyList(),
    val historyEnabled: Boolean = true,
    val isSavedData: Boolean = false,
    val isLoading: Boolean = false,
    val showDisableConfirmDialog: Boolean = false,
    val errorMessage: String? = null
)
