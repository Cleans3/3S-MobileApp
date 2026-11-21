package com.example.library.loans.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.library.loans.domain.Loan
import com.example.library.loans.domain.LoanRepository
import com.example.library.loans.domain.RenewOutcome
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val STOP_TIMEOUT_MS = 5_000L

/** Backs the active-loans and history screens for one member. */
class LoansViewModel(
    private val repository: LoanRepository,
    private val memberId: String,
) : ViewModel() {

    val activeLoans: StateFlow<List<Loan>> = repository.observeActiveLoans(memberId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val history: StateFlow<List<Loan>> = repository.observeHistory(memberId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val renewOutcomes = Channel<RenewOutcome>(Channel.BUFFERED)

    /** One-shot renewal results, consumed by the UI to show feedback. */
    val renewResults: Flow<RenewOutcome> = renewOutcomes.receiveAsFlow()

    fun renew(loanId: String) {
        viewModelScope.launch {
            renewOutcomes.send(repository.renewLoan(loanId, LocalDate.now()))
        }
    }
}

/** Backs the book-details screen for a single loan. */
class BookDetailViewModel(
    private val repository: LoanRepository,
    private val loanId: String,
) : ViewModel() {

    val loan: StateFlow<Loan?> = repository.observeLoan(loanId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private val renewOutcomes = Channel<RenewOutcome>(Channel.BUFFERED)

    val renewResults: Flow<RenewOutcome> = renewOutcomes.receiveAsFlow()

    fun renew() {
        viewModelScope.launch {
            renewOutcomes.send(repository.renewLoan(loanId, LocalDate.now()))
        }
    }
}
