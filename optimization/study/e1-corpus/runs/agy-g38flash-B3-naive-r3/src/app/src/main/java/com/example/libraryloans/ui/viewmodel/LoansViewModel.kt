package com.example.libraryloans.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.libraryloans.data.LoanRepository
import com.example.libraryloans.model.Book
import com.example.libraryloans.model.Loan
import com.example.libraryloans.model.LoanHistory
import com.example.libraryloans.model.Member
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

enum class LoanFilter {
    ALL,
    DUE_SOON,
    OVERDUE
}

class LoansViewModel(
    private val repository: LoanRepository = LoanRepository()
) : ViewModel() {

    val member: StateFlow<Member> = repository.member

    private val _selectedTab = MutableStateFlow(0) // 0: Active Loans, 1: History
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeFilter = MutableStateFlow(LoanFilter.ALL)
    val activeFilter: StateFlow<LoanFilter> = _activeFilter.asStateFlow()

    private val _selectedBook = MutableStateFlow<Book?>(null)
    val selectedBook: StateFlow<Book?> = _selectedBook.asStateFlow()

    private val _loanPendingRenewal = MutableStateFlow<Loan?>(null)
    val loanPendingRenewal: StateFlow<Loan?> = _loanPendingRenewal.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val filteredActiveLoans: StateFlow<List<Loan>> = combine(
        repository.activeLoans,
        _searchQuery,
        _activeFilter
    ) { loans, query, filter ->
        loans.filter { loan ->
            val matchesQuery = query.isBlank() ||
                    loan.book.title.contains(query, ignoreCase = true) ||
                    loan.book.author.contains(query, ignoreCase = true) ||
                    loan.book.genre.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                LoanFilter.ALL -> true
                LoanFilter.DUE_SOON -> loan.isDueSoon
                LoanFilter.OVERDUE -> loan.isOverdue
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filteredHistory: StateFlow<List<LoanHistory>> = combine(
        repository.loanHistory,
        _searchQuery
    ) { historyList, query ->
        historyList.filter { history ->
            query.isBlank() ||
                    history.book.title.contains(query, ignoreCase = true) ||
                    history.book.author.contains(query, ignoreCase = true) ||
                    history.book.genre.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalActiveLoansCount: StateFlow<Int> = repository.activeLoans.combine(MutableStateFlow(Unit)) { loans, _ ->
        loans.size
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val dueSoonCount: StateFlow<Int> = repository.activeLoans.combine(MutableStateFlow(Unit)) { loans, _ ->
        loans.count { it.isDueSoon || it.isOverdue }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val historyCount: StateFlow<Int> = repository.loanHistory.combine(MutableStateFlow(Unit)) { hist, _ ->
        hist.size
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChanged(filter: LoanFilter) {
        _activeFilter.value = filter
    }

    fun openBookDetail(book: Book) {
        _selectedBook.value = book
    }

    fun closeBookDetail() {
        _selectedBook.value = null
    }

    fun requestRenewal(loan: Loan) {
        _loanPendingRenewal.value = loan
    }

    fun dismissRenewalDialog() {
        _loanPendingRenewal.value = null
    }

    fun confirmRenewal() {
        val loan = _loanPendingRenewal.value ?: return
        val result = repository.renewLoan(loan.id)
        _loanPendingRenewal.value = null

        viewModelScope.launch {
            if (result.isSuccess) {
                val updated = result.getOrNull()
                _snackbarMessage.emit("Successfully renewed '${updated?.book?.title}' until ${updated?.dueDate}")
            } else {
                _snackbarMessage.emit(result.exceptionOrNull()?.message ?: "Failed to renew loan")
            }
        }
    }

    fun getActiveLoanForBook(bookId: String): Loan? {
        return repository.findActiveLoanByBookId(bookId)
    }

    fun getHistoryForBook(bookId: String): LoanHistory? {
        return repository.findHistoryByBookId(bookId)
    }
}
