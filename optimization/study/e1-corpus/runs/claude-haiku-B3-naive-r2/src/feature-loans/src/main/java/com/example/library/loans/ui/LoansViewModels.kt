package com.example.library.loans.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.library.loans.domain.Loan
import com.example.library.loans.domain.LoanRepository
import com.example.library.loans.domain.RenewalDecision
import com.example.library.loans.domain.RenewalPolicy
import com.example.library.loans.domain.RenewalResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class LoansUiState(
    val today: LocalDate = LocalDate.now(),
    val borrowed: List<Loan> = emptyList(),
    val history: List<Loan> = emptyList(),
    val isLoading: Boolean = true,
)

data class LoanDetailUiState(
    val loan: Loan? = null,
    val canRenew: Boolean = false,
    val isRenewing: Boolean = false,
    val lastResult: RenewalResult? = null,
)

class LoansListViewModel(
    repository: LoanRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    val uiState: StateFlow<LoansUiState> = repository.observeLoans()
        .map { loans ->
            LoansUiState(
                today = today(),
                borrowed = loans.filter { !it.isReturned }.sortedBy { it.dueDate },
                history = loans.filter { it.isReturned }.sortedByDescending { it.returnedOn },
                isLoading = false,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoansUiState())
}

class LoanDetailViewModel(
    private val loanId: String,
    private val repository: LoanRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val lastResult = MutableStateFlow<RenewalResult?>(null)
    private val isRenewing = MutableStateFlow(false)

    val uiState: StateFlow<LoanDetailUiState> = combine(
        repository.observeLoan(loanId),
        lastResult,
        isRenewing,
    ) { loan, result, busy ->
        LoanDetailUiState(
            loan = loan,
            canRenew = loan != null && RenewalPolicy.check(loan, today()) is RenewalDecision.Allowed,
            isRenewing = busy,
            lastResult = result,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoanDetailUiState())

    fun renew() {
        if (isRenewing.value) return
        viewModelScope.launch {
            isRenewing.value = true
            try {
                lastResult.value = repository.renewLoan(loanId, today())
            } finally {
                isRenewing.value = false
            }
        }
    }

    fun onResultShown() {
        lastResult.value = null
    }
}
