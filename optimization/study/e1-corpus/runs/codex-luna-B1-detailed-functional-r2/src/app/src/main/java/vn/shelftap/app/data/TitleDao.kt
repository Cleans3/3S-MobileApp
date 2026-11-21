package vn.shelftap.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TitleDao {
    @Query("SELECT * FROM titles ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<TitleEntity>>

    @Query("SELECT * FROM titles WHERE title LIKE '%' || :term || '%' OR authors LIKE '%' || :term || '%' OR isbn LIKE '%' || :term || '%' OR subjects LIKE '%' || :term || '%' ORDER BY title COLLATE NOCASE")
    fun observeSearch(term: String): Flow<List<TitleEntity>>

    @Query("SELECT * FROM titles WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<TitleEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(titles: List<TitleEntity>)

    @Query("SELECT COUNT(*) FROM titles")
    suspend fun count(): Int
}
