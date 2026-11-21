package vn.shelftap.app.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TitleDao {

    @Query("SELECT * FROM titles ORDER BY title COLLATE NOCASE, id")
    fun pagingAll(): PagingSource<Int, TitleEntity>

    @Query(
        """
        SELECT * FROM titles
        WHERE title LIKE :pattern ESCAPE '\'
           OR authors LIKE :pattern ESCAPE '\'
           OR isbn LIKE :pattern ESCAPE '\'
           OR subjects LIKE :pattern ESCAPE '\'
        ORDER BY title COLLATE NOCASE, id
        """
    )
    fun pagingSearch(pattern: String): PagingSource<Int, TitleEntity>

    @Query("SELECT * FROM titles WHERE id = :id")
    suspend fun getById(id: String): TitleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TitleEntity>)
}
