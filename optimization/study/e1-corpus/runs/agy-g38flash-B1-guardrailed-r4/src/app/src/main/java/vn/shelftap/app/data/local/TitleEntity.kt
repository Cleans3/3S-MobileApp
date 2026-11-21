package vn.shelftap.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey
import vn.shelftap.app.data.model.Title

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
    val authors: String,
    @ColumnInfo(name = "year")
    val year: Int,
    @ColumnInfo(name = "subjects")
    val subjects: String,
    @ColumnInfo(name = "cover_url")
    val coverUrl: String?,
    @ColumnInfo(name = "copies_available")
    val copiesAvailable: Int
) {
    fun toDomain(): Title = Title(
        id = id,
        isbn = isbn,
        title = title,
        authors = if (authors.isBlank()) emptyList() else authors.split(DELIMITER),
        year = year,
        subjects = if (subjects.isBlank()) emptyList() else subjects.split(DELIMITER),
        coverUrl = coverUrl,
        copiesAvailable = copiesAvailable
    )

    companion object {
        const val DELIMITER = "\u001F" // Unit separator delimiter

        fun fromDomain(title: Title): TitleEntity = TitleEntity(
            id = title.id,
            isbn = title.isbn,
            title = title.title,
            authors = title.authors.joinToString(DELIMITER),
            year = title.year,
            subjects = title.subjects.joinToString(DELIMITER),
            coverUrl = title.coverUrl,
            copiesAvailable = title.copiesAvailable
        )
    }
}

/**
 * FTS4 virtual table for fast full-text search over titles, authors, ISBN, and subjects.
 * Fulfills Rule 17.
 */
@Entity(tableName = "titles_fts")
@Fts4(contentEntity = TitleEntity::class)
data class TitleFtsEntity(
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "isbn")
    val isbn: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "authors")
    val authors: String,
    @ColumnInfo(name = "subjects")
    val subjects: String
)
