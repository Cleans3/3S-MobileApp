package vn.shelftap.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One catalogue title. List columns are stored as JSON by [Converters]. */
@Entity(
    tableName = "title",
    indices = [Index(value = ["sort_title"])],
)
data class TitleEntity(
    @PrimaryKey val id: String,
    val isbn: String,
    val title: String,
    val authors: List<String>,
    val year: Int,
    val subjects: List<String>,
    val coverUrl: String?,
    val copiesAvailable: Int,
    @ColumnInfo(name = "sort_title") val sortTitle: String,
)
