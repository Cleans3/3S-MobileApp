package vn.shelftap.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import vn.shelftap.app.data.model.Title

@Entity(
    tableName = "titles",
    indices = [
        Index(value = ["title"]),
        Index(value = ["isbn"]),
        Index(value = ["authorsSearch"]),
        Index(value = ["subjectsSearch"])
    ]
)
data class TitleEntity(
    @PrimaryKey
    val id: String,
    val isbn: String,
    val title: String,
    val authors: List<String>,
    val authorsSearch: String,
    val year: Int,
    val subjects: List<String>,
    val subjectsSearch: String,
    val coverUrl: String?,
    val copiesAvailable: Int
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
        fun fromDomain(title: Title): TitleEntity {
            return TitleEntity(
                id = title.id,
                isbn = title.isbn,
                title = title.title,
                authors = title.authors,
                authorsSearch = title.authors.joinToString(" "),
                year = title.year,
                subjects = title.subjects,
                subjectsSearch = title.subjects.joinToString(" "),
                coverUrl = title.coverUrl,
                copiesAvailable = title.copiesAvailable
            )
        }
    }
}
