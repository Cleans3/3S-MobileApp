package vn.shelftap.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import vn.shelftap.app.model.Title

private const val LIST_SEPARATOR = "\u001F"

/**
 * Local copy of a title. Lists are stored as one text column joined with a
 * control character, so that a plain LIKE query can search authors and subjects.
 */
@Entity(tableName = "titles")
data class TitleEntity(
    @PrimaryKey val id: String,
    val isbn: String,
    val title: String,
    val authors: String,
    val year: Int,
    val subjects: String,
    val coverUrl: String?,
    val copiesAvailable: Int,
) {
    fun toModel(): Title = Title(
        id = id,
        isbn = isbn,
        title = title,
        authors = authors.splitList(),
        year = year,
        subjects = subjects.splitList(),
        coverUrl = coverUrl,
        copiesAvailable = copiesAvailable,
    )

    companion object {
        fun from(t: Title): TitleEntity = TitleEntity(
            id = t.id,
            isbn = t.isbn,
            title = t.title,
            authors = t.authors.joinToString(LIST_SEPARATOR),
            year = t.year,
            subjects = t.subjects.joinToString(LIST_SEPARATOR),
            coverUrl = t.coverUrl,
            copiesAvailable = t.copiesAvailable,
        )
    }
}

private fun String.splitList(): List<String> =
    if (isEmpty()) emptyList() else split(LIST_SEPARATOR)
