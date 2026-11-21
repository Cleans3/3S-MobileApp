package com.example.librarycatalogue.data

/** Domain model used by the UI. */
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val year: Int?,
    val isbn: String?,
    val coverUrl: String?,
)

fun BookDto.toDomain() = Book(
    id = id,
    title = title,
    author = author,
    year = year,
    isbn = isbn,
    coverUrl = coverUrl,
)
