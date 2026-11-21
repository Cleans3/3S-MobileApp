package vn.shelftap.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Title(
    val id: String,
    val isbn: String,
    val title: String,
    val authors: List<String> = emptyList(),
    val year: Int,
    val subjects: List<String> = emptyList(),
    val coverUrl: String? = null,
    val copiesAvailable: Int = 0
)

@Serializable
data class TitleDto(
    val id: String,
    val isbn: String,
    val title: String,
    val authors: List<String> = emptyList(),
    val year: Int,
    val subjects: List<String> = emptyList(),
    val coverUrl: String? = null,
    val copiesAvailable: Int = 0
)
