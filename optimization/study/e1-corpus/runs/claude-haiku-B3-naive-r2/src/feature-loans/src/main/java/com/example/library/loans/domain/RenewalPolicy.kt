package com.example.library.loans.domain

import java.time.LocalDate

enum class DenialReason {
    ALREADY_RETURNED,
    OVERDUE,
    RENEWAL_LIMIT_REACHED,
}

sealed interface RenewalDecision {
    data class Allowed(val newDueDate: LocalDate) : RenewalDecision
    data class Denied(val reason: DenialReason) : RenewalDecision
}

/**
 * Business rules for renewing a loan:
 * - returned loans cannot be renewed,
 * - overdue loans cannot be renewed,
 * - at most [MAX_RENEWALS] renewals per loan,
 * - each renewal extends the due date by [RENEWAL_PERIOD_DAYS].
 */
object RenewalPolicy {
    const val MAX_RENEWALS = 2
    const val RENEWAL_PERIOD_DAYS = 14L

    fun check(loan: Loan, today: LocalDate): RenewalDecision = when {
        loan.isReturned -> RenewalDecision.Denied(DenialReason.ALREADY_RETURNED)
        loan.isOverdue(today) -> RenewalDecision.Denied(DenialReason.OVERDUE)
        loan.renewalCount >= MAX_RENEWALS -> RenewalDecision.Denied(DenialReason.RENEWAL_LIMIT_REACHED)
        else -> RenewalDecision.Allowed(loan.dueDate.plusDays(RENEWAL_PERIOD_DAYS))
    }
}
