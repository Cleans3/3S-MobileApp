package vn.shelftap.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/**
 * A title stored on the device. [searchText] is a pre-normalised (lower case, no diacritics)
 * copy of the title, authors, ISBN and subjects, so search is a single LIKE query.
 */
@Entity(tableName = "titles")
data class TitleEntity(
    @PrimaryKey val id: String,
    val isbn: String,
    val title: String,
    val authors: List<String>,
    val year: Int,
    val subjects: List<String>,
    val coverUrl: String?,
    val copiesAvailable: Int,
    val searchText: String,
)

fun TitleDto.toEntity(): TitleEntity = TitleEntity(
    id = id,
    isbn = isbn,
    title = title,
    authors = authors,
    year = year,
    subjects = subjects,
    coverUrl = coverUrl?.takeIf { it.isNotBlank() },
    copiesAvailable = copiesAvailable,
    searchText = SearchText.normalize(
        listOf(
            title,
            isbn,
            isbn.filter { it.isLetterOrDigit() },
            authors.joinToString(" "),
            subjects.joinToString(" "),
        ).joinToString(" "),
    ),
)

class StringListConverters {
    @TypeConverter
    fun encodeList(value: List<String>): String =
        ShelfTapJson.instance.encodeToString<List<String>>(value)

    @TypeConverter
    fun decodeList(value: String): List<String> =
        ShelfTapJson.instance.decodeFromString<List<String>>(value)
}
