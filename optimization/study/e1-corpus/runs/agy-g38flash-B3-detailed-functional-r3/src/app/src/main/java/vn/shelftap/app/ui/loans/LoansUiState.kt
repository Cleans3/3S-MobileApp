package vn.shelftap.app.ui.loans

import vn.shelftap.app.data.model.Loan

data class LoansUiState(
    val isLoading: Boolean = false,
    val loans: List<Loan> = emptyList(),
    val isSavedData: Boolean = false,
    val renewingLoanId: String? = null,
    val renewalErrors: Map<String, String> = emptyMap(),
    val errorMessage: String? = null
)
