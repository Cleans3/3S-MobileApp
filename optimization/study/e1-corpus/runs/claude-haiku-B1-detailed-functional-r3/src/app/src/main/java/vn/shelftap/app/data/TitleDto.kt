package vn.shelftap.app.data

import kotlinx.serialization.Serializable

/** One title as returned by the ShelfTap REST API. */
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

/** Response of GET /api/titles. */
@Serializable
data class PageDto(
    val items: List<TitleDto> = emptyList(),
    val page: Int = 0,
    val size: Int = 0,
    val total: Int = 0,
)
