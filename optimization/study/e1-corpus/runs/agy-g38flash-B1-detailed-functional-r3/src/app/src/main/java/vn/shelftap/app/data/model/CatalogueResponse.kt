package vn.shelftap.app.data.model

import com.google.gson.annotations.SerializedName

data class CatalogueResponse(
    @SerializedName("items")
    val items: List<Title>,
    @SerializedName("page")
    val page: Int,
    @SerializedName("size")
    val size: Int,
    @SerializedName("total")
    val total: Int
)
