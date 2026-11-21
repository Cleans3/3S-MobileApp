package com.example.libraryloans.model

import java.time.LocalDate

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val isbn: String,
    val genre: String,
    val publishYear: Int,
    val pageCount: Int,
    val publisher: String,
    val callNumber: String,
    val synopsis: String,
    val coverAccentHex: Long
)

enum class LoanStatus {
    ACTIVE,
    DUE_SOON,
    OVERDUE,
    RETURNED
}

data class Loan(
    val id: String,
    val book: Book,
    val borrowDate: LocalDate,
    val dueDate: LocalDate,
    val returnDate: LocalDate? = null,
    val renewalCount: Int = 0,
    val maxRenewals: Int = 2
) {
    val isReturned: Boolean
        get() = returnDate != null

    val daysRemaining: Long
        get() = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), dueDate)

    val status: LoanStatus
        get() {
            if (isReturned) return LoanStatus.RETURNED
            val days = daysRemaining
            return when {
                days < 0 -> LoanStatus.OVERDUE
                days <= 3 -> LoanStatus.DUE_SOON
                else -> LoanStatus.ACTIVE
            }
        }

    val canRenew: Boolean
        get() = !isReturned && renewalCount < maxRenewals && status != LoanStatus.OVERDUE
}
