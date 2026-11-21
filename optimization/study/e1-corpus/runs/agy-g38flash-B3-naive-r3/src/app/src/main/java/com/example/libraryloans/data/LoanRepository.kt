package com.example.libraryloans.data

import com.example.libraryloans.model.Book
import com.example.libraryloans.model.Loan
import com.example.libraryloans.model.LoanHistory
import com.example.libraryloans.model.Member
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

class LoanRepository {
    private val _member = MutableStateFlow(SampleData.currentMember)
    val member: StateFlow<Member> = _member.asStateFlow()

    private val _activeLoans = MutableStateFlow<List<Loan>>(SampleData.initialActiveLoans)
    val activeLoans: StateFlow<List<Loan>> = _activeLoans.asStateFlow()

    private val _loanHistory = MutableStateFlow<List<LoanHistory>>(SampleData.initialHistory)
    val loanHistory: StateFlow<List<LoanHistory>> = _loanHistory.asStateFlow()

    fun renewLoan(loanId: String): Result<Loan> {
        val loan = _activeLoans.value.find { it.id == loanId }
            ?: return Result.failure(IllegalArgumentException("Loan not found"))

        if (loan.isOverdue) {
            return Result.failure(IllegalStateException("Overdue items cannot be renewed online. Please visit library desk."))
        }

        if (loan.renewalsRemaining <= 0) {
            return Result.failure(IllegalStateException("Maximum renewal limit (2 times) reached for this book."))
        }

        val updatedLoan = loan.copy(
            dueDate = loan.dueDate.plusDays(14),
            renewalsRemaining = loan.renewalsRemaining - 1,
            renewalCount = loan.renewalCount + 1
        )

        _activeLoans.update { currentList ->
            currentList.map { if (it.id == loanId) updatedLoan else it }
        }

        return Result.success(updatedLoan)
    }

    fun returnLoan(loanId: String): Result<LoanHistory> {
        val loan = _activeLoans.value.find { it.id == loanId }
            ?: return Result.failure(IllegalArgumentException("Loan not found"))

        val newHistoryItem = LoanHistory(
            id = "H-" + System.currentTimeMillis(),
            book = loan.book,
            borrowDate = loan.borrowDate,
            returnDate = LocalDate.now(),
            userRating = 0,
            notes = "Returned on time."
        )

        _activeLoans.update { currentList -> currentList.filterNot { it.id == loanId } }
        _loanHistory.update { currentList -> listOf(newHistoryItem) + currentList }

        return Result.success(newHistoryItem)
    }

    fun findActiveLoanByBookId(bookId: String): Loan? {
        return _activeLoans.value.find { it.book.id == bookId }
    }

    fun findHistoryByBookId(bookId: String): LoanHistory? {
        return _loanHistory.value.find { it.book.id == bookId }
    }
}
