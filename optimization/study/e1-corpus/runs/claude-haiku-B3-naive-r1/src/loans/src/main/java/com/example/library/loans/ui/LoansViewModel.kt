package com.example.library.loans.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.library.loans.data.LoanRepository
import com.example.library.loans.domain.RejectReason
import com.example.library.loans.domain.RenewalResult
import com.example.library.loans.model.Loan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** One-off feedback shown to the member after a renewal attempt. */
sealed interface LoanMessage {
    data class Renewed(val title: String, val newDue: LocalDate) : LoanMessage
    data class Failed(val reason: RejectReason) : LoanMessage
}

data class LoansUiState(
    val today: LocalDate = LocalDate.now(),
    val active: List<Loan> = emptyList(),
    val history: List<Loan> = emptyList(),
    val message: LoanMessage? = null,
)

class LoansViewModel(
    private val repository: LoanRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val message = MutableStateFlow<LoanMessage?>(null)

    val uiState: StateFlow<LoansUiState> = combine(repository.observeLoans(), message) { loans, msg ->
        LoansUiState(
            today = today(),
            // Books still on loan, most urgent first.
            active = loans.filter { !it.isReturned }.sortedBy { it.dueOn },
            // Reading history, most recently returned first.
            history = loans.filter { it.isReturned }.sortedByDescending { it.returnedOn!! },
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoansUiState(today = today()))

    fun renew(loanId: String) {
        viewModelScope.launch {
            message.value = when (val result = repository.renew(loanId, today())) {
                is RenewalResult.Renewed -> LoanMessage.Renewed(result.loan.book.title, result.loan.dueOn)
                is RenewalResult.Rejected -> LoanMessage.Failed(result.reason)
            }
        }
    }

    fun consumeMessage() {
        message.value = null
    }
}
