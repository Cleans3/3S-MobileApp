package com.example.libraryloans.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class Loan(
    val id: String,
    val book: Book,
    val borrowDate: LocalDate,
    val dueDate: LocalDate,
    val renewalsRemaining: Int = 2,
    val renewalCount: Int = 0
) {
    val daysRemaining: Long
        get() = ChronoUnit.DAYS.between(LocalDate.now(), dueDate)

    val isOverdue: Boolean
        get() = LocalDate.now().isAfter(dueDate)

    val isDueSoon: Boolean
        get() = !isOverdue && daysRemaining <= 3

    val canRenew: Boolean
        get() = renewalsRemaining > 0 && !isOverdue
}
