package com.example.library.loans.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RenewalPolicyTest {

    private val today: LocalDate = LocalDate.of(2026, 11, 21)
    private val book = Book("b", "Title", "Author", "123", 2000, 100, "Description")

    private fun loan(
        due: LocalDate = today.plusDays(5),
        renewals: Int = 0,
        returned: LocalDate? = null,
    ) = Loan(
        id = "l",
        book = book,
        borrowedOn = today.minusDays(7),
        dueDate = due,
        returnedOn = returned,
        renewalCount = renewals,
    )

    @Test
    fun allowsRenewalAndExtendsDueDate() {
        val decision = RenewalPolicy.check(loan(), today)
        assertEquals(RenewalDecision.Allowed(today.plusDays(5 + 14)), decision)
    }

    @Test
    fun deniesOverdueLoan() {
        val decision = RenewalPolicy.check(loan(due = today.minusDays(1)), today)
        assertEquals(RenewalDecision.Denied(DenialReason.OVERDUE), decision)
    }

    @Test
    fun deniesWhenRenewalLimitReached() {
        val decision = RenewalPolicy.check(loan(renewals = RenewalPolicy.MAX_RENEWALS), today)
        assertEquals(RenewalDecision.Denied(DenialReason.RENEWAL_LIMIT_REACHED), decision)
    }

    @Test
    fun deniesReturnedLoan() {
        val decision = RenewalPolicy.check(loan(returned = today.minusDays(1)), today)
        assertEquals(RenewalDecision.Denied(DenialReason.ALREADY_RETURNED), decision)
    }
}
