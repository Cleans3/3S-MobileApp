package vn.shelftap.app.data.local

import androidx.room.Entity
import androidx.room.Fts4

@Entity(tableName = "titles_fts")
@Fts4
data class TitleFtsEntity(
    val id: String,
    val isbn: String,
    val title: String,
    val authors: String,
    val subjects: String
)
