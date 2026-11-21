package vn.shelftap.app.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TitleDao {

    @Query("SELECT * FROM titles ORDER BY title COLLATE NOCASE ASC")
    fun getPagedTitles(): PagingSource<Int, TitleEntity>

    @Query(
        """
        SELECT * FROM titles 
        WHERE title LIKE '%' || :query || '%' 
           OR authorsSearch LIKE '%' || :query || '%' 
           OR isbn LIKE '%' || :query || '%' 
           OR subjectsSearch LIKE '%' || :query || '%'
        ORDER BY title COLLATE NOCASE ASC
        """
    )
    fun searchPagedTitles(query: String): PagingSource<Int, TitleEntity>

    @Query("SELECT * FROM titles WHERE id = :id LIMIT 1")
    fun getTitleByIdFlow(id: String): Flow<TitleEntity?>

    @Query("SELECT * FROM titles WHERE id = :id LIMIT 1")
    suspend fun getTitleById(id: String): TitleEntity?

    @Query("SELECT COUNT(*) FROM titles")
    fun getTitleCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM titles")
    suspend fun getTitleCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(titles: List<TitleEntity>)

    @Query("DELETE FROM titles")
    suspend fun clearAll()
}
