package vn.shelftap.app.ui.loans

data class LoanUiItem(
    val loanId: String,
    val copyId: String,
    val titleId: String,
    val title: String,
    val barcode: String,
    val formattedDueDate: String,
    val daysLeftText: String,
    val isOverdue: Boolean,
    val renewable: Boolean,
    val isRenewing: Boolean = false
)

data class LoansUiState(
    val loans: List<LoanUiItem> = emptyList(),
    val isLoading: Boolean = false,
    val isSavedData: Boolean = false,
    val userMessage: String? = null,
    val errorMessage: String? = null
)
