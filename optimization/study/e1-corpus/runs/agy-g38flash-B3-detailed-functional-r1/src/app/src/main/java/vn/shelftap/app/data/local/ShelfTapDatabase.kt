package vn.shelftap.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LoanEntity::class, HistoryEntity::class], version = 1, exportSchema = false)
abstract class ShelfTapDatabase : RoomDatabase() {
    abstract fun loanDao(): LoanDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: ShelfTapDatabase? = null

        fun getInstance(context: Context): ShelfTapDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShelfTapDatabase::class.java,
                    "shelftap.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
