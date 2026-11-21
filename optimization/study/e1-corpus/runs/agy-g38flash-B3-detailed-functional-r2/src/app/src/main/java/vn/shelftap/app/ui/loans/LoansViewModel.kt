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
import vn.shelftap.app.data.repository.ShelfTapRepository
import vn.shelftap.app.util.RenewResult
import vn.shelftap.app.util.Resource

data class LoansUiState(
    val isLoading: Boolean = false,
    val loans: List<Loan> = emptyList(),
    val isSavedData: Boolean = false,
    val renewingLoanId: String? = null,
    val loanErrors: Map<String, String> = emptyMap(),
    val generalErrorMessage: String? = null,
    val userNotice: String? = null
)

class LoansViewModel(
    private val repository: ShelfTapRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoansUiState(isLoading = true))
    val uiState: StateFlow<LoansUiState> = _uiState.asStateFlow()

    init {
        loadLoans()
    }

    fun loadLoans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalErrorMessage = null) }
            when (val result = repository.getLoans()) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loans = result.data,
                            isSavedData = result.isSavedData,
                            generalErrorMessage = null
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSavedData = result.isSavedData,
                            generalErrorMessage = result.message
                        )
                    }
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun renewLoan(loanId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    renewingLoanId = loanId,
                    loanErrors = it.loanErrors - loanId
                )
            }

            when (val result = repository.renewLoan(loanId)) {
                is RenewResult.Success -> {
                    // Update the loan with the new due date and status
                    _uiState.update { state ->
                        val updatedList = state.loans.map {
                            if (it.loanId == loanId) result.updatedLoan else it
                        }
                        state.copy(
                            loans = updatedList,
                            renewingLoanId = null,
                            loanErrors = state.loanErrors - loanId,
                            userNotice = "Loan renewed successfully. New due date: ${result.updatedLoan.dueAt}"
                        )
                    }
                }
                is RenewResult.RenewalNotAllowed -> {
                    _uiState.update { state ->
                        state.copy(
                            renewingLoanId = null,
                            loanErrors = state.loanErrors + (loanId to result.message),
                            userNotice = result.message
                        )
                    }
                }
                is RenewResult.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            renewingLoanId = null,
                            loanErrors = state.loanErrors + (loanId to result.message),
                            userNotice = result.message
                        )
                    }
                }
            }
        }
    }

    fun clearUserNotice() {
        _uiState.update { it.copy(userNotice = null) }
    }

    class Factory(private val repository: ShelfTapRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoansViewModel::class.java)) {
                return LoansViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
