package vn.shelftap.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class HistoryItem(
    val titleId: String,
    val title: String,
    val outAt: String,
    val returnedAt: String? = null
)
