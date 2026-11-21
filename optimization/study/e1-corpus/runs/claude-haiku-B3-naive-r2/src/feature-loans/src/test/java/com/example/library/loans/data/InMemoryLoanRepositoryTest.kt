package com.example.library.loans.data

import com.example.library.loans.domain.Book
import com.example.library.loans.domain.DenialReason
import com.example.library.loans.domain.Loan
import com.example.library.loans.domain.RenewalPolicy
import com.example.library.loans.domain.RenewalResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InMemoryLoanRepositoryTest {

    private val today: LocalDate = LocalDate.of(2026, 11, 21)
    private val book = Book("b", "Title", "Author", "123", 2000, 100, "Description")

    private fun repoWith(vararg loans: Loan) = InMemoryLoanRepository(loans.toList())

    private fun loan(id: String, due: LocalDate, renewals: Int = 0) = Loan(
        id = id,
        book = book,
        borrowedOn = today.minusDays(7),
        dueDate = due,
        returnedOn = null,
        renewalCount = renewals,
    )

    @Test
    fun renewalIsStored() = runBlocking {
        val repo = repoWith(loan("l1", today.plusDays(3)))

        val result = repo.renewLoan("l1", today)

        assertTrue(result is RenewalResult.Renewed)
        val stored = repo.observeLoan("l1").first()
        assertEquals(1, stored?.renewalCount)
        assertEquals(today.plusDays(3 + RenewalPolicy.RENEWAL_PERIOD_DAYS), stored?.dueDate)
    }

    @Test
    fun rejectedRenewalLeavesLoanUnchanged() = runBlocking {
        val repo = repoWith(loan("l1", today.minusDays(1)))

        val result = repo.renewLoan("l1", today)

        assertEquals(RenewalResult.Rejected(DenialReason.OVERDUE), result)
        assertEquals(0, repo.observeLoan("l1").first()?.renewalCount)
    }

    @Test
    fun unknownLoanIsNotFound() = runBlocking {
        val repo = repoWith(loan("l1", today.plusDays(3)))

        assertEquals(RenewalResult.NotFound, repo.renewLoan("missing", today))
    }
}
