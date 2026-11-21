package vn.shelftap.app.ui.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.repository.LoansRepository
import vn.shelftap.app.data.repository.RenewalNotAllowedException

class LoansViewModel(
    private val loansRepository: LoansRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoansUiState(isLoading = true))
    val uiState: StateFlow<LoansUiState> = _uiState.asStateFlow()

    init {
        loadLoans()
    }

    fun loadLoans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = loansRepository.getLoans()
            result.fold(
                onSuccess = { (loans, isSaved) ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loans = loans,
                            isSavedData = isSaved,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to load loans"
                        )
                    }
                }
            )
        }
    }

    fun renewLoan(loanId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    renewingLoanId = loanId,
                    renewalErrors = it.renewalErrors - loanId
                )
            }

            val result = loansRepository.renewLoan(loanId)
            result.fold(
                onSuccess = { updatedLoan ->
                    _uiState.update { state ->
                        val updatedList = state.loans.map {
                            if (it.loanId == updatedLoan.loanId) updatedLoan else it
                        }
                        state.copy(
                            loans = updatedList,
                            renewingLoanId = null,
                            renewalErrors = state.renewalErrors - loanId
                        )
                    }
                },
                onFailure = { error ->
                    val errorMsg = if (error is RenewalNotAllowedException) {
                        "This loan cannot be renewed"
                    } else {
                        error.localizedMessage ?: "This loan cannot be renewed"
                    }

                    _uiState.update { state ->
                        state.copy(
                            renewingLoanId = null,
                            renewalErrors = state.renewalErrors + (loanId to errorMsg)
                        )
                    }
                }
            )
        }
    }

    fun dismissRenewalError(loanId: String) {
        _uiState.update { it.copy(renewalErrors = it.renewalErrors - loanId) }
    }

    companion object {
        fun provideFactory(loansRepository: LoansRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LoansViewModel(loansRepository) as T
                }
            }
    }
}
