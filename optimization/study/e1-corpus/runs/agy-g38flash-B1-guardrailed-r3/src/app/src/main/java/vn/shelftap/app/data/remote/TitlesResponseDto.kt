package vn.shelftap.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TitlesResponseDto(
    @SerialName("items")
    val items: List<TitleDto> = emptyList(),
    @SerialName("page")
    val page: Int = 0,
    @SerialName("size")
    val size: Int = 0,
    @SerialName("total")
    val total: Int = 0
)
