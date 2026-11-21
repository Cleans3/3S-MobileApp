package vn.shelftap.app

data class Title(
    val id: String, val isbn: String, val title: String, val authors: List<String>, val year: Int,
    val subjects: List<String>, val coverUrl: String?, val copiesAvailable: Int
)
