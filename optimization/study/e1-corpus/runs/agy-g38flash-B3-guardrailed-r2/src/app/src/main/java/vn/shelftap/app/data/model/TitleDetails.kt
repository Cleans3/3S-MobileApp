package vn.shelftap.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TitleDetails(
    @SerialName("titleId") val titleId: String,
    @SerialName("isbn") val isbn: String,
    @SerialName("title") val title: String,
    @SerialName("authors") val authors: List<String> = emptyList(),
    @SerialName("subjects") val subjects: List<String> = emptyList(),
    @SerialName("coverUrl") val coverUrl: String? = null,
    @SerialName("copiesAvailable") val copiesAvailable: Int,
    @SerialName("copiesTotal") val copiesTotal: Int
)
