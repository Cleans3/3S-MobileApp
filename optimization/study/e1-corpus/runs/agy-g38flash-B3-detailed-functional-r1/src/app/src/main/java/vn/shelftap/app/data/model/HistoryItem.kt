package vn.shelftap.app.data.model

import com.google.gson.annotations.SerializedName

data class HistoryItem(
    @SerializedName("titleId") val titleId: String,
    @SerializedName("title") val title: String,
    @SerializedName("outAt") val outAt: String,
    @SerializedName("returnedAt") val returnedAt: String? = null
)
