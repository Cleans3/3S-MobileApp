package vn.shelftap.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import vn.shelftap.app.data.remote.toStringList
import vn.shelftap.app.model.Title

/**
 * Offline copy of a title. Lists are stored as JSON text. [searchText] is a lowercase blob of
 * title, ISBN, authors and subjects, so one LIKE query covers all four search fields.
 */
@Entity(tableName = "titles", indices = [Index(value = ["title"])])
data class TitleEntity(
    @PrimaryKey val id: String,
    val isbn: String,
    val title: String,
    val authorsJson: String,
    val year: Int,
    val subjectsJson: String,
    val coverUrl: String?,
    val copiesAvailable: Int,
    val searchText: String,
)

fun Title.toEntity(): TitleEntity = TitleEntity(
    id = id,
    isbn = isbn,
    title = title,
    authorsJson = JSONArray(authors).toString(),
    year = year,
    subjectsJson = JSONArray(subjects).toString(),
    coverUrl = coverUrl,
    copiesAvailable = copiesAvailable,
    searchText = (listOf(title, isbn) + authors + subjects).joinToString("\n").lowercase(),
)

fun TitleEntity.toDomain(): Title = Title(
    id = id,
    isbn = isbn,
    title = title,
    authors = JSONArray(authorsJson).toStringList(),
    year = year,
    subjects = JSONArray(subjectsJson).toStringList(),
    coverUrl = coverUrl,
    copiesAvailable = copiesAvailable,
)
