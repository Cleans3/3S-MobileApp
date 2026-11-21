package com.example.library.loans.domain

import java.time.LocalDate

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val isbn: String,
    val publishedYear: Int,
    val pageCount: Int,
    val description: String,
)

/**
 * One borrowing of a [Book] by the member. A loan is active while [returnedOn] is null
 * and becomes part of the reading history once it has been returned.
 */
data class Loan(
    val id: String,
    val book: Book,
    val borrowedOn: LocalDate,
    val dueDate: LocalDate,
    val returnedOn: LocalDate?,
    val renewalCount: Int,
) {
    val isReturned: Boolean
        get() = returnedOn != null

    fun isOverdue(today: LocalDate): Boolean = !isReturned && today.isAfter(dueDate)
}
