package vn.shelftap.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TitleDao {
    @Query("SELECT * FROM title_details WHERE titleId = :titleId LIMIT 1")
    suspend fun getTitleById(titleId: String): TitleDetailEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTitle(title: TitleDetailEntity)
}
