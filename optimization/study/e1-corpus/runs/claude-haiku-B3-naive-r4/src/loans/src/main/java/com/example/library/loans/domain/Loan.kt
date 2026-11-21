package com.example.library.loans.domain

import java.time.LocalDate

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val isbn: String,
    val description: String,
    val pages: Int,
)

enum class LoanStatus { ACTIVE, RETURNED }

data class Loan(
    val id: String,
    val memberId: String,
    val book: Book,
    val borrowedOn: LocalDate,
    val dueDate: LocalDate,
    val renewCount: Int = 0,
    val status: LoanStatus = LoanStatus.ACTIVE,
    val returnedOn: LocalDate? = null,
)

fun Loan.isOverdue(today: LocalDate): Boolean =
    status == LoanStatus.ACTIVE && today.isAfter(dueDate)

object LoanRules {
    const val MAX_RENEWALS = 2
    const val RENEWAL_DAYS = 14L
}

enum class RenewRejection {
    ALREADY_RETURNED,
    OVERDUE,
    RENEWAL_LIMIT_REACHED,
    NOT_FOUND,
}

sealed interface RenewOutcome {
    data class Renewed(val loan: Loan) : RenewOutcome
    data class Rejected(val reason: RenewRejection) : RenewOutcome
}

/**
 * Applies the renewal rules to this loan as of [today].
 * A renewal extends the due date by [LoanRules.RENEWAL_DAYS] from the current due date.
 */
fun Loan.renew(today: LocalDate): RenewOutcome = when {
    status != LoanStatus.ACTIVE -> RenewOutcome.Rejected(RenewRejection.ALREADY_RETURNED)
    today.isAfter(dueDate) -> RenewOutcome.Rejected(RenewRejection.OVERDUE)
    renewCount >= LoanRules.MAX_RENEWALS -> RenewOutcome.Rejected(RenewRejection.RENEWAL_LIMIT_REACHED)
    else -> RenewOutcome.Renewed(
        copy(
            dueDate = dueDate.plusDays(LoanRules.RENEWAL_DAYS),
            renewCount = renewCount + 1,
        ),
    )
}
