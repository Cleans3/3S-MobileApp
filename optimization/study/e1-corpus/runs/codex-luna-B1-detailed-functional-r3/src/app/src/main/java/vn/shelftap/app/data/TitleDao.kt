package vn.shelftap.app.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TitleDao {
    @Query("SELECT * FROM titles WHERE (:query = '' OR lower(title) LIKE '%' || lower(:query) || '%' OR lower(isbn) LIKE '%' || lower(:query) || '%' OR lower(authorsJson) LIKE '%' || lower(:query) || '%' OR lower(subjectsJson) LIKE '%' || lower(:query) || '%') ORDER BY title COLLATE NOCASE")
    fun paging(query: String): PagingSource<Int, TitleEntity>

    @Query("SELECT * FROM titles WHERE id = :id LIMIT 1")
    fun observe(id: String): Flow<TitleEntity?>

    @Query("SELECT COUNT(*) FROM titles")
    suspend fun count(): Int

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TitleEntity>)
}
