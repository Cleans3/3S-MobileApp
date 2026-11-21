package vn.shelftap.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_items")
    suspend fun getAllHistory(): List<HistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(items: List<HistoryEntity>)

    @Query("DELETE FROM history_items")
    suspend fun clearHistory()

    @Transaction
    suspend fun replaceHistory(items: List<HistoryEntity>) {
        clearHistory()
        insertHistory(items)
    }
}
