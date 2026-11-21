package vn.shelftap.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TitlePageResponse(
    val items: List<TitleDto> = emptyList(),
    val page: Int = 0,
    val size: Int = 0,
    val total: Int = 0
)
