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

enum class LoanTab {
    ACTIVE,
    HISTORY
}

data class LoanWithBook(
    val loan: Loan,
    val book: Book
)

data class LoansUiState(
    val activeLoans: List<LoanWithBook> = emptyList(),
    val loanHistory: List<LoanWithBook> = emptyList(),
    val selectedTab: LoanTab = LoanTab.ACTIVE,
    val searchQuery: String = "",
    val selectedBook: Book? = null,
    val pendingRenewalLoan: LoanWithBook? = null,
    val totalActiveCount: Int = 0,
    val overdueCount: Int = 0,
    val dueSoonCount: Int = 0,
    val totalHistoryCount: Int = 0
)

class LoansViewModel(
    private val repository: LoanRepository = LoanRepository()
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(LoanTab.ACTIVE)
    val selectedTab: StateFlow<LoanTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedBook = MutableStateFlow<Book?>(null)
    val selectedBook: StateFlow<Book?> = _selectedBook.asStateFlow()

    private val _pendingRenewal = MutableStateFlow<LoanWithBook?>(null)
    val pendingRenewal: StateFlow<LoanWithBook?> = _pendingRenewal.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val uiState: StateFlow<LoansUiState> = combine(
        repository.loans,
        _selectedTab,
        _searchQuery,
        _selectedBook,
        _pendingRenewal
    ) { allLoans, tab, query, selectedBook, pendingRenewal ->
        val activeLoanItems = allLoans
            .filter { !it.isReturned }
            .mapNotNull { loan ->
                val book = repository.getBookById(loan.bookId)
                book?.let { LoanWithBook(loan, it) }
            }
            .sortedBy { it.loan.dueDate }

        val historyLoanItems = allLoans
            .filter { it.isReturned }
            .mapNotNull { loan ->
                val book = repository.getBookById(loan.bookId)
                book?.let { LoanWithBook(loan, it) }
            }
            .sortedByDescending { it.loan.returnDate }

        val filteredActive = if (query.isBlank()) {
            activeLoanItems
        } else {
            activeLoanItems.filter {
                it.book.title.contains(query, ignoreCase = true) ||
                it.book.author.contains(query, ignoreCase = true) ||
                it.book.genre.contains(query, ignoreCase = true)
            }
        }

        val filteredHistory = if (query.isBlank()) {
            historyLoanItems
        } else {
            historyLoanItems.filter {
                it.book.title.contains(query, ignoreCase = true) ||
                it.book.author.contains(query, ignoreCase = true) ||
                it.book.genre.contains(query, ignoreCase = true)
            }
        }

        val overdue = activeLoanItems.count { it.loan.daysRemaining() < 0 }
        val dueSoon = activeLoanItems.count {
            val days = it.loan.daysRemaining()
            days in 0..3
        }

        LoansUiState(
            activeLoans = filteredActive,
            loanHistory = filteredHistory,
            selectedTab = tab,
            searchQuery = query,
            selectedBook = selectedBook,
            pendingRenewalLoan = pendingRenewal,
            totalActiveCount = activeLoanItems.size,
            overdueCount = overdue,
            dueSoonCount = dueSoon,
            totalHistoryCount = historyLoanItems.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LoansUiState()
    )

    fun setTab(tab: LoanTab) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openBookDetails(book: Book) {
        _selectedBook.value = book
    }

    fun closeBookDetails() {
        _selectedBook.value = null
    }

    fun requestRenewal(loanWithBook: LoanWithBook) {
        if (!loanWithBook.loan.canRenew) {
            viewModelScope.launch {
                _snackbarMessage.emit("Cannot renew \"${loanWithBook.book.title}\": Maximum renewals reached.")
            }
            return
        }
        _pendingRenewal.value = loanWithBook
    }

    fun dismissRenewalDialog() {
        _pendingRenewal.value = null
    }

    fun confirmRenewal(loanId: String) {
        val result = repository.renewLoan(loanId, extensionDays = 14)
        _pendingRenewal.value = null
        viewModelScope.launch {
            if (result.isSuccess) {
                val renewedLoan = result.getOrNull()
                _snackbarMessage.emit("Loan renewed successfully! New due date: ${renewedLoan?.dueDate}")
            } else {
                _snackbarMessage.emit(result.exceptionOrNull()?.message ?: "Failed to renew loan")
            }
        }
    }

    fun getLoansForBook(bookId: String): List<Loan> {
        return repository.getLoansForBook(bookId)
    }
}
