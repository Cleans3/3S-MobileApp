package com.example.libraryloans.model

import java.time.LocalDate

data class Member(
    val id: String,
    val name: String,
    val cardNumber: String,
    val email: String,
    val memberSince: LocalDate,
    val cardExpiry: LocalDate,
    val maxLoansAllowed: Int = 10
)
