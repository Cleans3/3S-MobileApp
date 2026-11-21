package com.example.library.loans.model

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
    val book: Book,
    val borrowedOn: LocalDate,
    val dueOn: LocalDate,
    val renewalCount: Int = 0,
    val returnedOn: LocalDate? = null,
) {
    val isReturned: Boolean get() = returnedOn != null

    fun isOverdue(today: LocalDate): Boolean = !isReturned && dueOn.isBefore(today)
}
