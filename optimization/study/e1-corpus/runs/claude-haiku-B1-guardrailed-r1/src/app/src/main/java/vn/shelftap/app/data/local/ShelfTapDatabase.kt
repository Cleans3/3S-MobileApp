package vn.shelftap.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [TitleEntity::class, TitleSearchEntity::class, SyncStateEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class ShelfTapDatabase : RoomDatabase() {

    abstract fun titleDao(): TitleDao

    companion object {
        fun create(context: Context): ShelfTapDatabase =
            Room.databaseBuilder(context, ShelfTapDatabase::class.java, "shelftap.db").build()
    }
}
