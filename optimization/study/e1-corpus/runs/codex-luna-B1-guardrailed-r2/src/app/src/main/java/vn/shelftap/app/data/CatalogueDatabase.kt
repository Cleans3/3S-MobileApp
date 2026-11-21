package vn.shelftap.app.data

import androidx.room.Database
import androidx.room.Fts4
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Fts4(contentEntity = TitleEntity::class)
@androidx.room.Entity(tableName = "titles_fts")
data class TitleSearchIndex(
    val title: String,
    val isbn: String,
    val authorSearch: String,
    val subjectSearch: String
)

@Database(entities = [TitleEntity::class, TitleSearchIndex::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class CatalogueDatabase : RoomDatabase() {
    abstract fun catalogueDao(): CatalogueDao
}
