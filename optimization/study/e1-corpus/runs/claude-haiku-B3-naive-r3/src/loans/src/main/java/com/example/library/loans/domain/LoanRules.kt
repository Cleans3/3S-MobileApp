package com.example.library.loans.domain

import java.time.LocalDate

/**
 * Renewal policy:
 * - a loan can be renewed only while it is active (not returned) and not overdue;
 * - each renewal extends the due date by [RENEWAL_PERIOD_DAYS];
 * - at most [MAX_RENEWALS] renewals per loan.
 */
object LoanRules {
    const val MAX_RENEWALS = 2
    const val RENEWAL_PERIOD_DAYS = 14L

    fun renewalBlocker(loan: Loan, today: LocalDate): RenewalRejection? = when {
        loan.isReturned -> RenewalRejection.ALREADY_RETURNED
        loan.isOverdue(today) -> RenewalRejection.OVERDUE
        loan.renewalCount >= MAX_RENEWALS -> RenewalRejection.LIMIT_REACHED
        else -> null
    }

    fun renew(loan: Loan, today: LocalDate): RenewalResult {
        renewalBlocker(loan, today)?.let { return RenewalResult.Rejected(it) }
        return RenewalResult.Renewed(
            loan.copy(
                dueDate = loan.dueDate.plusDays(RENEWAL_PERIOD_DAYS),
                renewalCount = loan.renewalCount + 1,
            ),
        )
    }
}
