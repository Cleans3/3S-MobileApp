package com.library.catalogue.data.model

data class Book(
    val id: Long,
    val title: String,
    val author: String,
    val isbn: String,
    val genre: String,
    val publicationYear: Int,
    val description: String,
    val coverUrl: String,
    val coverColorHex: String,
    val totalCopies: Int,
    val availableCopies: Int,
    val rating: Float,
    val pages: Int
) {
    val isAvailable: Boolean
        get() = availableCopies > 0
}
