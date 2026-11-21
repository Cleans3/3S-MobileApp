package com.example.library.loans.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.library.loans.data.LoanRepository
import com.example.library.loans.domain.Book
import com.example.library.loans.domain.Loan
import com.example.library.loans.domain.RenewalResult
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MS = 5_000L

/** A loan together with the book it refers to (null if the book is unknown). */
data class LoanItem(val loan: Loan, val book: Book?)

data class MyLoansUiState(
    val items: List<LoanItem> = emptyList(),
    val lastRenewal: RenewalResult? = null,
)

class MyLoansViewModel(
    private val repository: LoanRepository,
    private val memberId: String,
) : ViewModel() {

    private val lastRenewal = MutableStateFlow<RenewalResult?>(null)

    val uiState: StateFlow<MyLoansUiState> = combine(
        repository.observeActiveLoans(memberId).map { loans ->
            loans.map { LoanItem(it, repository.getBook(it.bookId)) }
        },
        lastRenewal,
    ) { items, renewal ->
        MyLoansUiState(items = items, lastRenewal = renewal)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MyLoansUiState())

    fun renew(loanId: String) {
        viewModelScope.launch {
            lastRenewal.value = repository.renewLoan(loanId, LocalDate.now())
        }
    }

    fun onRenewalShown() {
        lastRenewal.value = null
    }
}

class HistoryViewModel(
    repository: LoanRepository,
    memberId: String,
) : ViewModel() {

    val items: StateFlow<List<LoanItem>> = repository.observeLoanHistory(memberId)
        .map { loans -> loans.map { LoanItem(it, repository.getBook(it.bookId)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())
}

data class BookDetailsUiState(
    val isLoading: Boolean = true,
    val book: Book? = null,
)

class BookDetailsViewModel(
    private val repository: LoanRepository,
    private val bookId: String,
) : ViewModel() {

    val uiState: StateFlow<BookDetailsUiState> = flow {
        emit(BookDetailsUiState(isLoading = false, book = repository.getBook(bookId)))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), BookDetailsUiState())
}
