package vn.shelftap.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TitleDetail(
    val titleId: String,
    val isbn: String,
    val title: String,
    val authors: List<String> = emptyList(),
    val subjects: List<String> = emptyList(),
    val coverUrl: String? = null,
    val copiesAvailable: Int,
    val copiesTotal: Int
)
