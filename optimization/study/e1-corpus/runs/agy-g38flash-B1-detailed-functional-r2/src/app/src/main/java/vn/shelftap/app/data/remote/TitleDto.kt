package vn.shelftap.app.data.remote

import com.google.gson.annotations.SerializedName
import vn.shelftap.app.model.Title

data class TitleDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("isbn")
    val isbn: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("authors")
    val authors: List<String>?,
    @SerializedName("year")
    val year: Int,
    @SerializedName("subjects")
    val subjects: List<String>?,
    @SerializedName("coverUrl")
    val coverUrl: String?,
    @SerializedName("copiesAvailable")
    val copiesAvailable: Int
) {
    fun toDomainModel(): Title {
        return Title(
            id = id,
            isbn = isbn,
            title = title,
            authors = authors ?: emptyList(),
            year = year,
            subjects = subjects ?: emptyList(),
            coverUrl = coverUrl,
            copiesAvailable = copiesAvailable
        )
    }
}
