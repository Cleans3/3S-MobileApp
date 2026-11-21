package vn.shelftap.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TitleEntity::class], version = 1, exportSchema = false)
abstract class ShelfTapDatabase : RoomDatabase() {
    abstract fun titleDao(): TitleDao

    companion object {
        fun create(context: Context) = Room.databaseBuilder(context, ShelfTapDatabase::class.java, "shelftap.db").build()
    }
}
