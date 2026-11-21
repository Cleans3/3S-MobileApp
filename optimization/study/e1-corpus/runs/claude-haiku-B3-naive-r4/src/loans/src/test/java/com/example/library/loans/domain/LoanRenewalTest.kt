package com.example.library.loans.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LoanRenewalTest {

    private val today: LocalDate = LocalDate.of(2026, 11, 21)
    private val book = Book("b1", "Title", "Author", "isbn", "description", 100)

    private fun loan(
        dueDate: LocalDate,
        renewCount: Int = 0,
        status: LoanStatus = LoanStatus.ACTIVE,
    ) = Loan(
        id = "l1",
        memberId = "m1",
        book = book,
        borrowedOn = today.minusDays(14),
        dueDate = dueDate,
        renewCount = renewCount,
        status = status,
        returnedOn = if (status == LoanStatus.RETURNED) today else null,
    )

    @Test
    fun renewalExtendsDueDateAndCountsRenewal() {
        val outcome = loan(today.plusDays(3)).renew(today)

        assertTrue(outcome is RenewOutcome.Renewed)
        val renewed = (outcome as RenewOutcome.Renewed).loan
        assertEquals(today.plusDays(3 + LoanRules.RENEWAL_DAYS), renewed.dueDate)
        assertEquals(1, renewed.renewCount)
    }

    @Test
    fun overdueLoanCannotBeRenewed() {
        val outcome = loan(today.minusDays(1)).renew(today)

        assertEquals(RenewOutcome.Rejected(RenewRejection.OVERDUE), outcome)
    }

    @Test
    fun renewalLimitIsEnforced() {
        val outcome = loan(today.plusDays(3), renewCount = LoanRules.MAX_RENEWALS).renew(today)

        assertEquals(RenewOutcome.Rejected(RenewRejection.RENEWAL_LIMIT_REACHED), outcome)
    }

    @Test
    fun returnedLoanCannotBeRenewed() {
        val outcome = loan(today.plusDays(3), status = LoanStatus.RETURNED).renew(today)

        assertEquals(RenewOutcome.Rejected(RenewRejection.ALREADY_RETURNED), outcome)
    }
}
