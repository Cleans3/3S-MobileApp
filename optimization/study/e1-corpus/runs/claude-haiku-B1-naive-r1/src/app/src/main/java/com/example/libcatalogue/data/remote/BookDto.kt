package com.example.libcatalogue.data.remote

/** One page of results as returned by the catalogue API. */
data class BookPageDto(
    val items: List<BookDto>? = null,
    val total: Int? = null
)

/** Wire format of a single book. Every field is nullable because the API may omit it. */
data class BookDto(
    val id: String? = null,
    val title: String? = null,
    val authors: List<String>? = null,
    val year: Int? = null,
    val coverUrl: String? = null,
    val available: Boolean? = null
)
