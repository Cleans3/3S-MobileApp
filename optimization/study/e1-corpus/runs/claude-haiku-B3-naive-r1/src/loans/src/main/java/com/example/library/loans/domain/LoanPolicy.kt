package com.example.library.loans.domain

import com.example.library.loans.model.Loan
import java.time.LocalDate

enum class RejectReason { NOT_FOUND, ALREADY_RETURNED, OVERDUE, RENEWAL_LIMIT_REACHED }

sealed interface RenewalResult {
    data class Renewed(val loan: Loan) : RenewalResult
    data class Rejected(val reason: RejectReason) : RenewalResult
}

/**
 * Renewal rules:
 * - returned loans cannot be renewed,
 * - overdue loans cannot be renewed,
 * - at most [MAX_RENEWALS] renewals per loan,
 * - each renewal extends the due date by [RENEWAL_PERIOD_DAYS] days.
 */
object LoanPolicy {
    const val MAX_RENEWALS = 2
    const val RENEWAL_PERIOD_DAYS = 14L

    fun renew(loan: Loan, today: LocalDate): RenewalResult = when {
        loan.isReturned -> RenewalResult.Rejected(RejectReason.ALREADY_RETURNED)
        loan.isOverdue(today) -> RenewalResult.Rejected(RejectReason.OVERDUE)
        loan.renewalCount >= MAX_RENEWALS -> RenewalResult.Rejected(RejectReason.RENEWAL_LIMIT_REACHED)
        else -> RenewalResult.Renewed(
            loan.copy(
                dueOn = loan.dueOn.plusDays(RENEWAL_PERIOD_DAYS),
                renewalCount = loan.renewalCount + 1,
            ),
        )
    }
}
