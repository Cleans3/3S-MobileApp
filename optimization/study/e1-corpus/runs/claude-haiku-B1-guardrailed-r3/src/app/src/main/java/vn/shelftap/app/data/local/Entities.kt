package vn.shelftap.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

/** One catalogue title. List fields are stored joined with the unit separator U+001F. */
@Entity(tableName = "titles", indices = [Index(value = ["title"])])
data class TitleEntity(
    @PrimaryKey val id: String,
    val isbn: String,
    val title: String,
    val authors: String,
    val year: Int,
    val subjects: String,
    val coverUrl: String?,
    val copiesAvailable: Int,
)

/** Full-text index over the searchable fields. The id is stored but not indexed. */
@Fts4(notIndexed = ["titleId"])
@Entity(tableName = "title_search")
data class TitleSearchEntity(
    @ColumnInfo(name = "titleId") val titleId: String,
    val title: String,
    val authors: String,
    val isbn: String,
    val subjects: String,
)

/** Single row (id = 0) recording how far the catalogue download has got. */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: Int,
    val nextPage: Int,
    val total: Int,
    val complete: Boolean,
)
