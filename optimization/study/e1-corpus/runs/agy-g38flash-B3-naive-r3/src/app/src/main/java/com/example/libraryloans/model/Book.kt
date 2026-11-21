package com.example.libraryloans.model

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val isbn: String,
    val genre: String,
    val description: String,
    val publisher: String,
    val publishedYear: Int,
    val pageCount: Int,
    val coverColorHex: Long,
    val callNumber: String
)
