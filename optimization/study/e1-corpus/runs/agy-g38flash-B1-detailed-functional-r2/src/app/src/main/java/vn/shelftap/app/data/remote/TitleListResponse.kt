package vn.shelftap.app.data.remote

import com.google.gson.annotations.SerializedName

data class TitleListResponse(
    @SerializedName("items")
    val items: List<TitleDto>,
    @SerializedName("page")
    val page: Int,
    @SerializedName("size")
    val size: Int,
    @SerializedName("total")
    val total: Int
)
