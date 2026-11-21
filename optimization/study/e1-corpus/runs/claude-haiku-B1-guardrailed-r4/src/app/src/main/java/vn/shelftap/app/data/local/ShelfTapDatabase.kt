package vn.shelftap.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TitleEntity::class, TitleSearchEntity::class, SyncStateEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ShelfTapDatabase : RoomDatabase() {
    abstract fun catalogueDao(): CatalogueDao
}
