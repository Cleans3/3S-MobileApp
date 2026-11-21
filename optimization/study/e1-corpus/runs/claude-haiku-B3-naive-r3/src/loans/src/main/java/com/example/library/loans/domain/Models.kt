package com.example.library.loans.domain

import java.time.LocalDate

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val isbn: String,
    val pages: Int,
    val description: String,
)

data class Loan(
    val id: String,
    val bookId: String,
    val memberId: String,
    val borrowedOn: LocalDate,
    val dueDate: LocalDate,
    val returnedOn: LocalDate? = null,
    val renewalCount: Int = 0,
) {
    val isReturned: Boolean
        get() = returnedOn != null

    fun isOverdue(today: LocalDate): Boolean = !isReturned && dueDate.isBefore(today)
}

enum class RenewalRejection {
    ALREADY_RETURNED,
    OVERDUE,
    LIMIT_REACHED,
    NOT_FOUND,
}

sealed interface RenewalResult {
    data class Renewed(val loan: Loan) : RenewalResult
    data class Rejected(val reason: RenewalRejection) : RenewalResult
}
