package com.example.library.loans.data

import com.example.library.loans.domain.Book
import com.example.library.loans.domain.Loan
import com.example.library.loans.domain.LoanRules
import com.example.library.loans.domain.RenewalRejection
import com.example.library.loans.domain.RenewalResult
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface LoanRepository {
    /** Loans of [memberId] that have not been returned yet, earliest due date first. */
    fun observeActiveLoans(memberId: String): Flow<List<Loan>>

    /** Returned loans of [memberId], most recently returned first. */
    fun observeLoanHistory(memberId: String): Flow<List<Loan>>

    suspend fun getBook(bookId: String): Book?

    suspend fun renewLoan(loanId: String, today: LocalDate): RenewalResult
}

/** Process-lifetime repository backed by [SampleData]; no network involved. */
class InMemoryLoanRepository(
    books: List<Book> = SampleData.books(),
    loans: List<Loan> = SampleData.loans(LocalDate.now()),
) : LoanRepository {

    private val booksById = books.associateBy { it.id }
    private val loansState = MutableStateFlow(loans)
    private val mutex = Mutex()

    override fun observeActiveLoans(memberId: String): Flow<List<Loan>> =
        loansState.map { all ->
            all.filter { it.memberId == memberId && !it.isReturned }
                .sortedBy { it.dueDate }
        }

    override fun observeLoanHistory(memberId: String): Flow<List<Loan>> =
        loansState.map { all ->
            all.filter { it.memberId == memberId && it.isReturned }
                .sortedByDescending { it.returnedOn }
        }

    override suspend fun getBook(bookId: String): Book? = booksById[bookId]

    override suspend fun renewLoan(loanId: String, today: LocalDate): RenewalResult =
        mutex.withLock {
            val current = loansState.value.firstOrNull { it.id == loanId }
                ?: return RenewalResult.Rejected(RenewalRejection.NOT_FOUND)

            val result = LoanRules.renew(current, today)
            if (result is RenewalResult.Renewed) {
                loansState.update { list ->
                    list.map { if (it.id == loanId) result.loan else it }
                }
            }
            result
        }
}
