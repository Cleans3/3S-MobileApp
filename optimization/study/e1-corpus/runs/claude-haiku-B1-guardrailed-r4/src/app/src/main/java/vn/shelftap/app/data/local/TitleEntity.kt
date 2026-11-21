package vn.shelftap.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

/** One catalogue title. Authors and subjects are stored joined by [LIST_SEPARATOR]. */
@Entity(tableName = "titles", indices = [Index(value = ["title"])])
data class TitleEntity(
    @PrimaryKey val id: String,
    val isbn: String,
    @ColumnInfo(name = "title", collate = ColumnInfo.NOCASE) val title: String,
    val authors: String,
    val year: Int,
    val subjects: String,
    val coverUrl: String?,
    val copiesAvailable: Int,
)

/**
 * Full-text index for search. Rows are written by [CatalogueDao.upsertTitles]
 * together with the title row. [titleId] links back to [TitleEntity.id] and is not indexed.
 */
@Fts4(notIndexed = ["titleId"])
@Entity(tableName = "title_search")
data class TitleSearchEntity(
    val titleId: String,
    val title: String,
    val authors: String,
    val isbn: String,
    val subjects: String,
)

/** Single-row progress marker for the resumable catalogue download (id is always 1). */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: Int = 1,
    val nextPage: Int,
    val complete: Boolean,
)
