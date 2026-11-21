package vn.shelftap.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import vn.shelftap.app.data.api.TitleDto
import vn.shelftap.app.domain.model.Title

@Entity(tableName = "titles")
data class TitleEntity(
    @PrimaryKey
    val id: String,
    val isbn: String,
    val title: String,
    val authors: List<String>,
    val year: Int,
    val subjects: List<String>,
    val coverUrl: String?,
    val copiesAvailable: Int
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

    companion object {
        fun fromDto(dto: TitleDto): TitleEntity = TitleEntity(
            id = dto.id,
            isbn = dto.isbn,
            title = dto.title,
            authors = dto.authors,
            year = dto.year,
            subjects = dto.subjects,
            coverUrl = dto.coverUrl,
            copiesAvailable = dto.copiesAvailable
        )

        fun fromDomain(domain: Title): TitleEntity = TitleEntity(
            id = domain.id,
            isbn = domain.isbn,
            title = domain.title,
            authors = domain.authors,
            year = domain.year,
            subjects = domain.subjects,
            coverUrl = domain.coverUrl,
            copiesAvailable = domain.copiesAvailable
        )
    }
}
