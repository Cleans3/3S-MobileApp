package vn.shelftap.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * FTS4 virtual table for full-text search over catalogue titles (Rule 17).
 */
@Entity(tableName = "titles_fts")
@Fts4
data class TitleFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Int? = null,
    val titleId: String,
    val title: String,
    val authors: String,
    val isbn: String,
    val subjects: String
)
