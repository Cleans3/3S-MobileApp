package vn.shelftap.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import vn.shelftap.app.model.Title

@Entity(
    tableName = "titles",
    indices = [
        Index(value = ["title"]),
        Index(value = ["isbn"]),
        Index(value = ["year"]),
        Index(value = ["authorsSearchString"]),
        Index(value = ["subjectsSearchString"])
    ]
)
data class TitleEntity(
    @PrimaryKey
    val id: String,
    val isbn: String,
    val title: String,
    val authors: List<String>,
    val year: Int,
    val subjects: List<String>,
    val coverUrl: String?,
    val copiesAvailable: Int,
    val authorsSearchString: String,
    val subjectsSearchString: String,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): Title {
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
                year = title.year,
                subjects = title.subjects,
                coverUrl = title.coverUrl,
                copiesAvailable = title.copiesAvailable,
                authorsSearchString = title.authors.joinToString(" ").lowercase(),
                subjectsSearchString = title.subjects.joinToString(" ").lowercase()
            )
        }
    }
}
