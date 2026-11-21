package com.example.libraryloans.model

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val isbn: String,
    val publisher: String,
    val publishYear: Int,
    val genre: String,
    val pages: Int,
    val description: String,
    val coverAccentHex: Long = 0xFF1E3A8A,
    val shelfLocation: String = "Section B, Floor 2"
)
