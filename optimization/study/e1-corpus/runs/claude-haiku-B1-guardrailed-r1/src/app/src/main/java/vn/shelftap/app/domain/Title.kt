package vn.shelftap.app.domain

/** A catalogue title as the UI sees it. */
data class Title(
    val id: String,
    val isbn: String,
    val title: String,
    val authors: List<String>,
    val year: Int,
    val subjects: List<String>,
    val coverUrl: String?,
    val copiesAvailable: Int,
) {
    val isAvailable: Boolean
        get() = copiesAvailable > 0
}
