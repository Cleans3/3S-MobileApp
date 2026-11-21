package com.example.libraryloans.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.libraryloans.data.LoanRepository
import com.example.libraryloans.model.Book
import com.example.libraryloans.model.Loan
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    object Main : Screen()
    data class BookDetails(val book: Book, val currentLoan: Loan?) : Screen()
}

class LoansViewModel(
    private val repository: LoanRepository = LoanRepository()
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0) // 0: Active, 1: History
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Main)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    // Dialog state for confirming renewal
    private val _pendingRenewalLoan = MutableStateFlow<Loan?>(null)
    val pendingRenewalLoan: StateFlow<Loan?> = _pendingRenewalLoan.asStateFlow()

    val activeLoans: StateFlow<List<Loan>> = repository.allLoans
        .combine(MutableStateFlow(Unit)) { loans, _ ->
            loans.filter { !it.isReturned }
                .sortedBy { it.dueDate }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loanHistory: StateFlow<List<Loan>> = repository.allLoans
        .combine(_historySearchQuery) { loans, query ->
            loans.filter { it.isReturned }
                .filter {
                    query.isBlank() ||
                    it.book.title.contains(query, ignoreCase = true) ||
                    it.book.author.contains(query, ignoreCase = true) ||
                    it.book.genre.contains(query, ignoreCase = true)
                }
                .sortedByDescending { it.returnDate }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun openBookDetails(book: Book, loan: Loan?) {
        _currentScreen.value = Screen.BookDetails(book = book, currentLoan = loan)
    }

    fun navigateBack() {
        _currentScreen.value = Screen.Main
    }

    fun updateHistorySearchQuery(query: String) {
        _historySearchQuery.value = query
    }

    fun promptRenewLoan(loan: Loan) {
        _pendingRenewalLoan.value = loan
    }

    fun dismissRenewDialog() {
        _pendingRenewalLoan.value = null
    }

    fun confirmRenewal(loanId: String) {
        _pendingRenewalLoan.value = null
        viewModelScope.launch {
            val result = repository.renewLoan(loanId)
            result.onSuccess { updatedLoan ->
                _snackbarEvent.emit("Successfully renewed '${updatedLoan.book.title}' until ${updatedLoan.dueDate}")
                // If currently viewing details of this book, update screen
                val cur = _currentScreen.value
                if (cur is Screen.BookDetails && cur.book.id == updatedLoan.book.id) {
                    _currentScreen.value = Screen.BookDetails(cur.book, updatedLoan)
                }
            }.onFailure { error ->
                _snackbarEvent.emit(error.message ?: "Failed to renew loan")
            }
        }
    }
}
