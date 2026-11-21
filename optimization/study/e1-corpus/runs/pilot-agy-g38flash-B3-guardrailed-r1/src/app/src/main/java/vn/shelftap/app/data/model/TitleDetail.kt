package vn.shelftap.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TitleDetail(
    @SerialName("titleId") val titleId: String,
    @SerialName("isbn") val isbn: String,
    @SerialName("title") val title: String,
    @SerialName("authors") val authors: List<String>,
    @SerialName("subjects") val subjects: List<String>,
    @SerialName("coverUrl") val coverUrl: String? = null,
    @SerialName("copiesAvailable") val copiesAvailable: Int,
    @SerialName("copiesTotal") val copiesTotal: Int
)
