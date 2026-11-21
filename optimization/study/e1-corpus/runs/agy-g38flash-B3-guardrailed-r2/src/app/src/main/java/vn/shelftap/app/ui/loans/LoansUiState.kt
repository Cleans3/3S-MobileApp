package vn.shelftap.app.ui.loans

data class LoanItemUi(
    val loanId: String,
    val titleId: String,
    val title: String,
    val formattedDueDate: String,
    val daysLeft: Long,
    val isOverdue: Boolean,
    val daysLeftText: String,
    val renewable: Boolean,
    val isRenewing: Boolean = false
)

data class LoansUiState(
    val isLoading: Boolean = false,
    val loans: List<LoanItemUi> = emptyList(),
    val isSavedData: Boolean = false,
    val userMessage: String? = null,
    val error: String? = null
)
