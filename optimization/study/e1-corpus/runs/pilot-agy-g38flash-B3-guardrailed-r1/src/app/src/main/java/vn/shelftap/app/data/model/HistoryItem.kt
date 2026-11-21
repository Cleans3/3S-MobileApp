package vn.shelftap.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HistoryItem(
    @SerialName("titleId") val titleId: String,
    @SerialName("title") val title: String,
    @SerialName("outAt") val outAt: String,
    @SerialName("returnedAt") val returnedAt: String? = null
)
