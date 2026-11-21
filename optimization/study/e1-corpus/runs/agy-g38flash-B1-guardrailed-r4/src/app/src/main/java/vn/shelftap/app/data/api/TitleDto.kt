package vn.shelftap.app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TitleDto(
    @SerialName("id") val id: String,
    @SerialName("isbn") val isbn: String,
    @SerialName("title") val title: String,
    @SerialName("authors") val authors: List<String> = emptyList(),
    @SerialName("year") val year: Int,
    @SerialName("subjects") val subjects: List<String> = emptyList(),
    @SerialName("coverUrl") val coverUrl: String? = null,
    @SerialName("copiesAvailable") val copiesAvailable: Int = 0
)

@Serializable
data class TitlesResponseDto(
    @SerialName("items") val items: List<TitleDto> = emptyList(),
    @SerialName("page") val page: Int = 0,
    @SerialName("size") val size: Int = 50,
    @SerialName("total") val total: Int = 0
)
