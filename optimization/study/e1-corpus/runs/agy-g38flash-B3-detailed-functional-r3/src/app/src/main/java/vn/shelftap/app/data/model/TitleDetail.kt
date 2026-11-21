package vn.shelftap.app.data.model

import com.google.gson.annotations.SerializedName

data class TitleDetail(
    @SerializedName("titleId") val titleId: String,
    @SerializedName("isbn") val isbn: String,
    @SerializedName("title") val title: String,
    @SerializedName("authors") val authors: List<String> = emptyList(),
    @SerializedName("subjects") val subjects: List<String> = emptyList(),
    @SerializedName("coverUrl") val coverUrl: String? = null,
    @SerializedName("copiesAvailable") val copiesAvailable: Int = 0,
    @SerializedName("copiesTotal") val copiesTotal: Int = 0
)
