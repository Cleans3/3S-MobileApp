package com.example.libraryloans.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class LoanHistory(
    val id: String,
    val book: Book,
    val borrowDate: LocalDate,
    val returnDate: LocalDate,
    val userRating: Int = 0,
    val notes: String = ""
) {
    val daysBorrowed: Long
        get() = ChronoUnit.DAYS.between(borrowDate, returnDate)
}
