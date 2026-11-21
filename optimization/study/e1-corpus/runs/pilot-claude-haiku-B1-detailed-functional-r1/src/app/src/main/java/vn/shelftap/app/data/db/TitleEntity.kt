package vn.shelftap.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

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
    /** Lower-cased title, ISBN, authors and subjects; used by search. */
    @ColumnInfo(name = "search_text") val searchText: String,
)

class ListConverters {
    @TypeConverter
    fun fromList(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun toList(value: String): List<String> = Json.decodeFromString(value)
}
