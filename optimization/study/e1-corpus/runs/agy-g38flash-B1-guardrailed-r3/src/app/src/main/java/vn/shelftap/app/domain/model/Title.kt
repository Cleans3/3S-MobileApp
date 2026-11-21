package vn.shelftap.app.domain.model

data class Title(
    val id: String,
    val isbn: String,
    val title: String,
    val authors: List<String>,
    val year: Int,
    val subjects: List<String>,
    val coverUrl: String?,
    val copiesAvailable: Int
) {
    val isAvailable: Boolean
        get() = copiesAvailable > 0

    val formattedAuthors: String
        get() = if (authors.isEmpty()) "Unknown Author" else authors.joinToString(", ")
}
