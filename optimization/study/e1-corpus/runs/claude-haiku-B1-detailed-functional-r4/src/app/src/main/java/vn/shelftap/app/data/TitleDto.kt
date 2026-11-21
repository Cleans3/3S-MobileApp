package vn.shelftap.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Shared JSON configuration for the REST API and the Room list converters. */
object ShelfTapJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
}

/** One title as returned by GET /api/titles/{id} and inside list pages. */
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
data class TitlePageDto(
    val items: List<TitleDto> = emptyList(),
    val page: Int = 0,
    val size: Int = 0,
    val total: Int = 0,
)
