package vn.shelftap.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProfileDao {
    @Query("SELECT * FROM member_profile LIMIT 1")
    suspend fun getProfile(): MemberProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: MemberProfileEntity)

    @Query("UPDATE member_profile SET historyEnabled = :enabled")
    suspend fun updateHistoryEnabled(enabled: Boolean)

    @Query("DELETE FROM member_profile")
    suspend fun clearProfile()
}
