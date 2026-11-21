package com.example.libcatalogue.data

/** Book as used by the UI. Built from [com.example.libcatalogue.data.remote.BookDto]. */
data class Book(
    val id: String,
    val title: String,
    val authors: List<String>,
    val year: Int?,
    val coverUrl: String?,
    val available: Boolean
)
