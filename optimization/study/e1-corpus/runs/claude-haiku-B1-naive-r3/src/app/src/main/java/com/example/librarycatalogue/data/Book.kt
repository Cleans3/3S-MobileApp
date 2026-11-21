package com.example.librarycatalogue.data

import com.google.gson.annotations.SerializedName

/** Domain model used by the UI. */
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val year: Int?,
    val isbn: String?,
    val coverUrl: String?,
)

/** Wire format returned by the library API. */
internal data class BookDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("author") val author: String?,
    @SerializedName("year") val year: Int?,
    @SerializedName("isbn") val isbn: String?,
    @SerializedName("cover_url") val coverUrl: String?,
) {
    fun toDomain(): Book = Book(
        id = id,
        title = title.ifBlank { "Untitled" },
        author = author?.takeIf { it.isNotBlank() } ?: "Unknown author",
        year = year,
        isbn = isbn,
        coverUrl = coverUrl?.takeIf { it.isNotBlank() },
    )
}
