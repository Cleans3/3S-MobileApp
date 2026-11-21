package vn.shelftap.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [TitleEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class CatalogueDatabase : RoomDatabase() {

    abstract fun titleDao(): TitleDao

    companion object {
        fun create(context: Context): CatalogueDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                CatalogueDatabase::class.java,
                "catalogue.db",
            ).build()
    }
}
