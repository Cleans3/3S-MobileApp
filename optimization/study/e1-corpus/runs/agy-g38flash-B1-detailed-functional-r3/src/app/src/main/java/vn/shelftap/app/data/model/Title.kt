package vn.shelftap.app.data.model

import com.google.gson.annotations.SerializedName

data class Title(
    @SerializedName("id")
    val id: String,
    @SerializedName("isbn")
    val isbn: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("authors")
    val authors: List<String>,
    @SerializedName("year")
    val year: Int,
    @SerializedName("subjects")
    val subjects: List<String>,
    @SerializedName("coverUrl")
    val coverUrl: String? = null,
    @SerializedName("copiesAvailable")
    val copiesAvailable: Int
)
