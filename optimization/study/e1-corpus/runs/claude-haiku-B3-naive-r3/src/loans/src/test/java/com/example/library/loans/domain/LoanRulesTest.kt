package com.example.library.loans.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoanRulesTest {

    private val today: LocalDate = LocalDate.of(2026, 11, 21)

    private fun loan(
        dueDate: LocalDate = today.plusDays(3),
        renewalCount: Int = 0,
        returnedOn: LocalDate? = null,
    ) = Loan(
        id = "loan-test",
        bookId = "book-test",
        memberId = "member-test",
        borrowedOn = today.minusDays(10),
        dueDate = dueDate,
        returnedOn = returnedOn,
        renewalCount = renewalCount,
    )

    @Test
    fun activeLoanCanBeRenewedAndDueDateExtended() {
        val result = LoanRules.renew(loan(), today)

        assertTrue(result is RenewalResult.Renewed)
        val renewed = (result as RenewalResult.Renewed).loan
        assertEquals(today.plusDays(3 + LoanRules.RENEWAL_PERIOD_DAYS), renewed.dueDate)
        assertEquals(1, renewed.renewalCount)
    }

    @Test
    fun loanDueTodayIsNotOverdueAndCanBeRenewed() {
        assertNull(LoanRules.renewalBlocker(loan(dueDate = today), today))
    }

    @Test
    fun overdueLoanCannotBeRenewed() {
        assertEquals(
            RenewalRejection.OVERDUE,
            LoanRules.renewalBlocker(loan(dueDate = today.minusDays(1)), today),
        )
    }

    @Test
    fun loanCannotBeRenewedBeyondTheLimit() {
        val atLimit = loan(renewalCount = LoanRules.MAX_RENEWALS)
        assertEquals(RenewalRejection.LIMIT_REACHED, LoanRules.renewalBlocker(atLimit, today))
        assertTrue(LoanRules.renew(atLimit, today) is RenewalResult.Rejected)
    }

    @Test
    fun returnedLoanCannotBeRenewed() {
        assertEquals(
            RenewalRejection.ALREADY_RETURNED,
            LoanRules.renewalBlocker(loan(returnedOn = today.minusDays(1)), today),
        )
    }
}
