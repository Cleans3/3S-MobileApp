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
import vn.shelftap.app.data.repository.RenewResult
import vn.shelftap.app.data.repository.Resource

data class LoansUiState(
    val loans: List<Loan> = emptyList(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val renewingLoanId: String? = null,
    val userMessage: String? = null
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
            _uiState.update { it.copy(isLoading = true) }
            when (val res = repository.getLoans()) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            loans = res.data,
                            isOffline = res.isOffline,
                            isLoading = false
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userMessage = res.message
                        )
                    }
                }
            }
        }
    }

    fun renewLoan(loanId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(renewingLoanId = loanId) }
            when (val result = repository.renewLoan(loanId)) {
                is RenewResult.Success -> {
                    _uiState.update { state ->
                        val updatedList = state.loans.map { loan ->
                            if (loan.loanId == loanId) result.updatedLoan else loan
                        }
                        state.copy(
                            loans = updatedList,
                            renewingLoanId = null
                        )
                    }
                }
                is RenewResult.RenewalNotAllowed -> {
                    _uiState.update {
                        it.copy(
                            renewingLoanId = null,
                            userMessage = "This loan cannot be renewed"
                        )
                    }
                }
                is RenewResult.Error -> {
                    _uiState.update {
                        it.copy(
                            renewingLoanId = null,
                            userMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
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
