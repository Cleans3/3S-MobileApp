package vn.shelftap.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Domain and API model representing a library title in ShelfTap.
 *
 * @property id Unique identifier of the title.
 * @property isbn International Standard Book Number.
 * @property title Name/title of the book.
 * @property authors List of author names.
 * @property year Publication year.
 * @property subjects Categories or subject classifications.
 * @property coverUrl HTTPS URL of a 600 by 900 JPEG cover image, or null if absent.
 * @property copiesAvailable Number of physical copies currently available on shelves.
 */
data class Title(
    @SerializedName("id")
    val id: String,

    @SerializedName("isbn")
    val isbn: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("authors")
    val authors: List<String>,

    @SerializedName("year")
    val year: Int,

    @SerializedName("subjects")
    val subjects: List<String>,

    @SerializedName("coverUrl")
    val coverUrl: String? = null,

    @SerializedName("copiesAvailable")
    val copiesAvailable: Int
) {
    /**
     * True if at least one copy is currently on the shelf.
     */
    val isAvailable: Boolean
        get() = copiesAvailable > 0

    /**
     * Formatted comma-separated list of authors.
     */
    val authorsFormatted: String
        get() = if (authors.isEmpty()) "Unknown Author" else authors.joinToString(", ")
}
