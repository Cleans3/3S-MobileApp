package vn.shelftap.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import vn.shelftap.app.data.model.Title

/**
 * Room entity representing a book title cached locally for offline browsing and search.
 * Includes indexed and flattened search helper columns to allow lightning-fast SQL queries
 * across 100,000+ titles without an active internet connection.
 */
@Entity(
    tableName = "titles",
    indices = [
        Index(value = ["title"]),
        Index(value = ["isbn"]),
        Index(value = ["year"]),
        Index(value = ["authorsSearch"]),
        Index(value = ["subjectsSearch"])
    ]
)
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

    @ColumnInfo(name = "coverUrl")
    val coverUrl: String?,

    @ColumnInfo(name = "copiesAvailable")
    val copiesAvailable: Int,

    // Joined lowercase strings for fast substring searches (LIKE '%...%')
    @ColumnInfo(name = "authorsSearch")
    val authorsSearch: String,

    @ColumnInfo(name = "subjectsSearch")
    val subjectsSearch: String
) {
    fun toDomain(): Title {
        return Title(
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

    companion object {
        fun fromDomain(domain: Title): TitleEntity {
            return TitleEntity(
                id = domain.id,
                isbn = domain.isbn,
                title = domain.title,
                authors = domain.authors,
                year = domain.year,
                subjects = domain.subjects,
                coverUrl = domain.coverUrl,
                copiesAvailable = domain.copiesAvailable,
                authorsSearch = domain.authors.joinToString(" ").lowercase(),
                subjectsSearch = domain.subjects.joinToString(" ").lowercase()
            )
        }
    }
}
