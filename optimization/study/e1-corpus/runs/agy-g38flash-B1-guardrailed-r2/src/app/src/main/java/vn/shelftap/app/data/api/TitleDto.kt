package vn.shelftap.app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import vn.shelftap.app.domain.model.Title

@Serializable
data class TitleListResponse(
    @SerialName("items") val items: List<TitleDto>,
    @SerialName("page") val page: Int,
    @SerialName("size") val size: Int,
    @SerialName("total") val total: Int
)

@Serializable
data class TitleDto(
    @SerialName("id") val id: String,
    @SerialName("isbn") val isbn: String,
    @SerialName("title") val title: String,
    @SerialName("authors") val authors: List<String> = emptyList(),
    @SerialName("year") val year: Int,
    @SerialName("subjects") val subjects: List<String> = emptyList(),
    @SerialName("coverUrl") val coverUrl: String? = null,
    @SerialName("copiesAvailable") val copiesAvailable: Int
) {
    fun toDomain(): Title = Title(
        id = id,
        isbn = isbn,
        title = title,
        authors = authors,
        year = year,
        subjects = subjects,
        coverUrl = coverUrl,
        copiesAvailable = copiesAvailable
    )
}
