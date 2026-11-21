package vn.shelftap.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

data class TitleDto(
    val id: String,
    val isbn: String,
    val title: String,
    val authors: List<String> = emptyList(),
    val year: Int,
    val subjects: List<String> = emptyList(),
    val coverUrl: String? = null,
    val copiesAvailable: Int
)

data class TitlesPage(
    val items: List<TitleDto>,
    val page: Int,
    val size: Int,
    val total: Int
)

@Entity(tableName = "titles")
data class TitleEntity(
    @PrimaryKey val id: String,
    val isbn: String,
    val title: String,
    val authorsJson: String,
    val year: Int,
    val subjectsJson: String,
    val coverUrl: String?,
    val copiesAvailable: Int
) {
    fun toTitle() = TitleDto(id, isbn, title, JsonLists.decode(authorsJson), year, JsonLists.decode(subjectsJson), coverUrl, copiesAvailable)
}

object JsonLists {
    fun encode(values: List<String>): String = org.json.JSONArray(values).toString()
    fun decode(value: String): List<String> = runCatching {
        val array = org.json.JSONArray(value)
        List(array.length()) { array.getString(it) }
    }.getOrDefault(emptyList())
}

fun TitleDto.toEntity() = TitleEntity(id, isbn, title, JsonLists.encode(authors), year, JsonLists.encode(subjects), coverUrl, copiesAvailable)
