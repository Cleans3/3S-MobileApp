package vn.shelftap.app.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TitleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(titles: List<TitleEntity>)

    @Query("SELECT * FROM titles ORDER BY title COLLATE NOCASE ASC")
    fun getAllTitlesPagingSource(): PagingSource<Int, TitleEntity>

    @Query(
        """
        SELECT * FROM titles 
        WHERE LOWER(title) LIKE '%' || :query || '%'
           OR LOWER(isbn) LIKE '%' || :query || '%'
           OR authorsSearchString LIKE '%' || :query || '%'
           OR subjectsSearchString LIKE '%' || :query || '%'
        ORDER BY title COLLATE NOCASE ASC
        """
    )
    fun searchTitlesPagingSource(query: String): PagingSource<Int, TitleEntity>

    @Query("SELECT * FROM titles WHERE id = :id")
    fun getTitleById(id: String): Flow<TitleEntity?>

    @Query("SELECT * FROM titles WHERE id = :id")
    suspend fun getTitleByIdSync(id: String): TitleEntity?

    @Query("SELECT COUNT(*) FROM titles")
    fun getTitleCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM titles")
    suspend fun getTitleCount(): Int

    @Query("DELETE FROM titles")
    suspend fun clearAll()
}
