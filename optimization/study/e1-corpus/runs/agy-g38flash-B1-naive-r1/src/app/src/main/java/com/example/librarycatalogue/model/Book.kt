package com.example.librarycatalogue.model

data class Book(
    val id: Long,
    val title: String,
    val author: String,
    val isbn: String,
    val genre: String,
    val publicationYear: Int,
    val pageCount: Int,
    val description: String,
    val shelfLocation: String,
    val isAvailable: Boolean,
    val coverColorSeed: Long,
    val coverImageUrl: String? = null
)
