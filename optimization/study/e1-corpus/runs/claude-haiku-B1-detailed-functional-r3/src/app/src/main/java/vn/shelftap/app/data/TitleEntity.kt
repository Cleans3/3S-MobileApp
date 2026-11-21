package vn.shelftap.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

/**
 * A title stored on the device. [searchText] is a lower-cased concatenation of the
 * title, authors, ISBN and subjects, so that search is a single LIKE on one column.
 * [syncedAt] records when the row was last confirmed by the server.
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
    val syncedAt: Long,
)

fun TitleDto.toEntity(syncedAt: Long): TitleEntity = TitleEntity(
    id = id,
    isbn = isbn,
    title = title,
    authors = authors,
    year = year,
    subjects = subjects,
    coverUrl = coverUrl,
    copiesAvailable = copiesAvailable,
    searchText = buildString {
        append(title).append('\n')
        authors.forEach { append(it).append('\n') }
        append(isbn).append('\n')
        subjects.forEach { append(it).append('\n') }
    }.lowercase(),
    syncedAt = syncedAt,
)

/** Stores string lists as JSON text in SQLite. */
class Converters {
    @TypeConverter
    fun listToJson(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun jsonToList(value: String): List<String> = Json.decodeFromString(value)
}
