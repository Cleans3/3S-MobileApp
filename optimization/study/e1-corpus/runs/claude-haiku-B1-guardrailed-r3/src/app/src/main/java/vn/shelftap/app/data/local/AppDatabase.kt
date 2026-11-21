package vn.shelftap.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TitleEntity::class, TitleSearchEntity::class, SyncStateEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun titleDao(): TitleDao
    abstract fun titleSearchDao(): TitleSearchDao
    abstract fun syncStateDao(): SyncStateDao
}
