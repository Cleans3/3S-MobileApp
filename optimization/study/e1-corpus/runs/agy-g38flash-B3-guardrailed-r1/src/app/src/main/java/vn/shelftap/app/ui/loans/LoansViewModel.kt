package vn.shelftap.app.ui.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.model.Loan
import vn.shelftap.app.data.repository.LibraryRepository
import vn.shelftap.app.data.repository.RenewalNotAllowedException
import vn.shelftap.app.util.DateFormatterUtil

data class LoanItemUiState(
    val loan: Loan,
    val formattedDueDate: String,
    val daysLeftText: String,
    val isOverdue: Boolean,
    val isRenewing: Boolean = false,
    val errorMessage: String? = null
)

data class LoansUiState(
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val loans: List<LoanItemUiState> = emptyList(),
    val error: String? = null,
    val infoMessage: String? = null
)

class LoansViewModel(
    private val repository: LibraryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoansUiState(isLoading = true))
    val uiState: StateFlow<LoansUiState> = _uiState.asStateFlow()

    init {
        loadLoans()
    }

    fun loadLoans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val cachedResult = repository.getLoans()
                val uiItems = cachedResult.data.map { loan ->
                    mapLoanToUiState(loan)
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isOffline = cachedResult.isOffline,
                        loans = uiItems,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load loans"
                    )
                }
            }
        }
    }

    fun renewLoan(loanId: String) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    loans = state.loans.map {
                        if (it.loan.loanId == loanId) it.copy(isRenewing = true, errorMessage = null) else it
                    }
                )
            }

            try {
                val updatedLoan = repository.renewLoan(loanId)
                val updatedItem = mapLoanToUiState(updatedLoan)
                _uiState.update { state ->
                    state.copy(
                        loans = state.loans.map {
                            if (it.loan.loanId == loanId) updatedItem else it
                        },
                        infoMessage = "Loan renewed successfully"
                    )
                }
            } catch (e: RenewalNotAllowedException) {
                _uiState.update { state ->
                    state.copy(
                        loans = state.loans.map {
                            if (it.loan.loanId == loanId) {
                                it.copy(isRenewing = false, errorMessage = "This loan cannot be renewed")
                            } else it
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        loans = state.loans.map {
                            if (it.loan.loanId == loanId) {
                                it.copy(isRenewing = false, errorMessage = e.message ?: "Renewal failed")
                            } else it
                        }
                    )
                }
            }
        }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    private fun mapLoanToUiState(loan: Loan): LoanItemUiState {
        val dueStatus = DateFormatterUtil.calculateDueStatus(loan.dueAt)
        return LoanItemUiState(
            loan = loan,
            formattedDueDate = dueStatus.formattedDueDate,
            daysLeftText = dueStatus.daysLeftText,
            isOverdue = dueStatus.isOverdue
        )
    }

    companion object {
        fun provideFactory(repository: LibraryRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LoansViewModel(repository) as T
                }
            }
    }
}
