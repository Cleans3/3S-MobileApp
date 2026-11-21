package vn.shelftap.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import vn.shelftap.app.domain.model.Title

@Entity(tableName = "titles")
data class TitleEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "isbn")
    val isbn: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "authors")
    val authors: List<String>,
    @ColumnInfo(name = "year")
    val year: Int,
    @ColumnInfo(name = "subjects")
    val subjects: List<String>,
    @ColumnInfo(name = "cover_url")
    val coverUrl: String?,
    @ColumnInfo(name = "copies_available")
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
        fun fromDomain(title: Title): TitleEntity = TitleEntity(
            id = title.id,
            isbn = title.isbn,
            title = title.title,
            authors = title.authors,
            year = title.year,
            subjects = title.subjects,
            coverUrl = title.coverUrl,
            copiesAvailable = title.copiesAvailable
        )
    }
}
