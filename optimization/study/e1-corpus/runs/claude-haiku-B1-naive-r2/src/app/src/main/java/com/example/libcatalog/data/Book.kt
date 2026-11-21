package com.example.libcatalog.data

/**
 * A book in the library catalogue, as returned by the catalogue API.
 * Gson fills these fields by name, so the property names must match the JSON keys.
 */
data class Book(
    val id: String,
    val title: String,
    val author: String? = null,
    val isbn: String? = null,
    val year: Int? = null,
    val coverUrl: String? = null,
)
