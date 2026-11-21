package vn.shelftap.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ListConverters {
    private val gson = Gson()
    @TypeConverter fun fromList(value: List<String>): String = gson.toJson(value)
    @TypeConverter fun toList(value: String): List<String> = gson.fromJson(value, object : TypeToken<List<String>>() {}.type) ?: emptyList()
}

@Database(entities = [TitleEntity::class], version = 1, exportSchema = false)
@TypeConverters(ListConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun titleDao(): TitleDao
    companion object {
        fun create(context: Context) = Room.databaseBuilder(context, AppDatabase::class.java, "shelftap.db").build()
    }
}
