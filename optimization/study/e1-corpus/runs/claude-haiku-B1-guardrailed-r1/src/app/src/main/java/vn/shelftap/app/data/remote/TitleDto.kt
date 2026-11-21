package vn.shelftap.app.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class TitleDto(
    val id: String,
    val isbn: String = "",
    val title: String = "",
    val authors: List<String> = emptyList(),
    val year: Int = 0,
    val subjects: List<String> = emptyList(),
    val coverUrl: String? = null,
    val copiesAvailable: Int = 0,
)

@Serializable
data class TitlePageDto(
    val items: List<TitleDto> = emptyList(),
    val page: Int = 0,
    val size: Int = 0,
    val total: Int = 0,
)
