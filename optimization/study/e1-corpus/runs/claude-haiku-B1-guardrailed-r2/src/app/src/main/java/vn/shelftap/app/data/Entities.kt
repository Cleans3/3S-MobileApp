package vn.shelftap.app.data

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Index
import androidx.room.PrimaryKey

/** One catalogue title. Catalogue data is public, so it is stored as plain rows. */
@Entity(tableName = "titles", indices = [Index(value = ["sortTitle"])])
data class TitleEntity(
    @PrimaryKey val id: String,
    val isbn: String,
    val title: String,
    /** Lower-cased title, indexed so the list can be paged in order without sorting 100k rows. */
    val sortTitle: String,
    val authors: List<String>,
    val year: Int,
    val subjects: List<String>,
    val coverUrl: String?,
    val copiesAvailable: Int
)

/**
 * Full-text index used for search. Holds the searchable text only.
 * Rows are joined back to [TitleEntity] by [id].
 */
@Fts4(notIndexed = ["id"], tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "title_search")
data class TitleSearchEntity(
    val id: String,
    val title: String,
    val authors: String,
    val isbn: String,
    val subjects: String
)

/** Single-row progress of the catalogue download, committed in the same transaction as each page. */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: Int = 1,
    val nextPage: Int,
    val total: Int,
    val complete: Boolean
)
