package com.example.libraryloans.data

import com.example.libraryloans.model.Book
import com.example.libraryloans.model.Loan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class LoanRepository(
    private val booksMap: Map<String, Book> = SampleDataProvider.books,
    initialLoans: List<Loan> = SampleDataProvider.initialLoans()
) {
    private val _loans = MutableStateFlow(initialLoans)
    val loans: Flow<List<Loan>> = _loans.asStateFlow()

    fun getActiveLoans(): Flow<List<Loan>> {
        return _loans.map { list ->
            list.filter { !it.isReturned }
                .sortedBy { it.dueDate }
        }
    }

    fun getLoanHistory(): Flow<List<Loan>> {
        return _loans.map { list ->
            list.filter { it.isReturned }
                .sortedByDescending { it.returnDate }
        }
    }

    fun getBookById(bookId: String): Book? {
        return booksMap[bookId]
    }

    fun getLoansForBook(bookId: String): List<Loan> {
        return _loans.value.filter { it.bookId == bookId }
    }

    fun renewLoan(loanId: String, extensionDays: Long = 14): Result<Loan> {
        val currentList = _loans.value
        val loanIndex = currentList.indexOfFirst { it.id == loanId }
        if (loanIndex == -1) {
            return Result.failure(IllegalArgumentException("Loan not found"))
        }

        val loan = currentList[loanIndex]
        if (!loan.canRenew) {
            return Result.failure(IllegalStateException("Maximum renewals reached (${loan.maxRenewals}) or loan is returned"))
        }

        // Extend due date by extensionDays from current due date (or from today if overdue)
        val baseDate = if (loan.dueDate.isBefore(LocalDate.now())) LocalDate.now() else loan.dueDate
        val updatedLoan = loan.copy(
            dueDate = baseDate.plusDays(extensionDays),
            renewalCount = loan.renewalCount + 1
        )

        val updatedList = currentList.toMutableList()
        updatedList[loanIndex] = updatedLoan
        _loans.value = updatedList

        return Result.success(updatedLoan)
    }

    fun returnLoan(loanId: String): Result<Loan> {
        val currentList = _loans.value
        val loanIndex = currentList.indexOfFirst { it.id == loanId }
        if (loanIndex == -1) {
            return Result.failure(IllegalArgumentException("Loan not found"))
        }

        val loan = currentList[loanIndex]
        val updatedLoan = loan.copy(
            returnDate = LocalDate.now()
        )

        val updatedList = currentList.toMutableList()
        updatedList[loanIndex] = updatedLoan
        _loans.value = updatedList

        return Result.success(updatedLoan)
    }
}
