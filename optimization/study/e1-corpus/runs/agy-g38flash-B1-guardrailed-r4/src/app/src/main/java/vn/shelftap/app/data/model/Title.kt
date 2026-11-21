package vn.shelftap.app.data.model

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
    val isAvailable: Boolean get() = copiesAvailable > 0
    val authorsFormatted: String get() = if (authors.isEmpty()) "Unknown Author" else authors.joinToString(", ")
}
