package vn.shelftap.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.PrimaryKey

/**
 * Full-text index over [TitleEntity].
 *
 * The rows are written with SQL in [TitleDao]: the FTS rowid is set to the rowid of the
 * matching `title` row, so a re-sync can delete old index rows by rowid instead of scanning.
 * [rowId] exists only because Room requires an FTS entity to declare a `rowid` primary key.
 */
@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61, notIndexed = ["docId"])
@Entity(tableName = "title_fts")
data class TitleSearchEntity(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowId: Int,
    val docId: String,
    val titleText: String,
    val authorsText: String,
    val isbn: String,
    val subjectsText: String,
)
