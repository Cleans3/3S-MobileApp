package vn.shelftap.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "titles_fts")
@Fts4
data class TitleFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Int? = null,
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "isbn")
    val isbn: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "authors")
    val authors: String,
    @ColumnInfo(name = "subjects")
    val subjects: String
)
