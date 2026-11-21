package com.example.libraryloans.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class LoanStatus {
    ACTIVE,
    DUE_SOON,
    OVERDUE,
    RETURNED
}

data class Loan(
    val id: String,
    val bookId: String,
    val borrowDate: LocalDate,
    val dueDate: LocalDate,
    val returnDate: LocalDate? = null,
    val renewalCount: Int = 0,
    val maxRenewals: Int = 2,
    val userRating: Int? = null, // 1 to 5 stars if returned and rated
    val userNotes: String? = null
) {
    val isReturned: Boolean
        get() = returnDate != null

    fun getEffectiveStatus(today: LocalDate = LocalDate.now()): LoanStatus {
        if (isReturned) return LoanStatus.RETURNED
        val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate)
        return when {
            daysUntilDue < 0 -> LoanStatus.OVERDUE
            daysUntilDue <= 3 -> LoanStatus.DUE_SOON
            else -> LoanStatus.ACTIVE
        }
    }

    fun daysRemaining(today: LocalDate = LocalDate.now()): Long {
        return ChronoUnit.DAYS.between(today, dueDate)
    }

    val canRenew: Boolean
        get() = !isReturned && renewalCount < maxRenewals
}
