package vn.shelftap.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [TitleEntity::class], version = 1, exportSchema = false)
@TypeConverters(StringListConverters::class)
abstract class ShelfTapDatabase : RoomDatabase() {
    abstract fun titleDao(): TitleDao
}
