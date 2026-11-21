package vn.shelftap.slop

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val tags: List<String>,
)
