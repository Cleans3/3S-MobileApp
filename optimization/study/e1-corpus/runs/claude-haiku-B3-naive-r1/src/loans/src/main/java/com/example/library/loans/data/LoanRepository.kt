package com.example.library.loans.data

import com.example.library.loans.domain.LoanPolicy
import com.example.library.loans.domain.RejectReason
import com.example.library.loans.domain.RenewalResult
import com.example.library.loans.model.Book
import com.example.library.loans.model.Loan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate

/** Access to the member's loans. Implementations may be backed by a server or a local database. */
interface LoanRepository {
    /** All loans of the member, active and returned. */
    fun observeLoans(): Flow<List<Loan>>

    /** Book details for a book the member has borrowed. */
    fun findBook(bookId: String): Book?

    suspend fun renew(loanId: String, today: LocalDate): RenewalResult
}

/** Simple in-memory implementation, used by default and for previews/tests. */
class InMemoryLoanRepository(
    seed: List<Loan> = SampleData.loans(),
) : LoanRepository {

    private val loans: MutableStateFlow<List<Loan>> = MutableStateFlow(seed)
    private val mutex = Mutex()

    override fun observeLoans(): StateFlow<List<Loan>> = loans.asStateFlow()

    override fun findBook(bookId: String): Book? =
        loans.value.firstOrNull { it.book.id == bookId }?.book

    override suspend fun renew(loanId: String, today: LocalDate): RenewalResult = mutex.withLock {
        val current = loans.value.firstOrNull { it.id == loanId }
            ?: return@withLock RenewalResult.Rejected(RejectReason.NOT_FOUND)

        val result = LoanPolicy.renew(current, today)
        if (result is RenewalResult.Renewed) {
            loans.update { list -> list.map { if (it.id == loanId) result.loan else it } }
        }
        result
    }
}
