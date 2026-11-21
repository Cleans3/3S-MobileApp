package vn.shelftap.app.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TitleDao {

    @Upsert
    suspend fun upsertAll(titles: List<TitleEntity>)

    @Query("SELECT * FROM titles WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<TitleEntity?>

    @Query("SELECT * FROM titles ORDER BY title COLLATE NOCASE, id")
    fun pagingAll(): PagingSource<Int, TitleEntity>

    @Query("SELECT * FROM titles WHERE searchText LIKE :pattern ESCAPE '!' ORDER BY title COLLATE NOCASE, id")
    fun pagingSearch(pattern: String): PagingSource<Int, TitleEntity>
}
