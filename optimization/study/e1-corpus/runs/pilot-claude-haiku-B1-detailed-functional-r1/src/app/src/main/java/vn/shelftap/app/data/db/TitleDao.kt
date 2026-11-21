package vn.shelftap.app.data.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface TitleDao {

    @Upsert
    suspend fun upsertAll(titles: List<TitleEntity>)

    @Query("SELECT * FROM titles ORDER BY title COLLATE NOCASE, id")
    fun pageAll(): PagingSource<Int, TitleEntity>

    @Query("SELECT * FROM titles WHERE search_text LIKE :pattern ESCAPE '!' ORDER BY title COLLATE NOCASE, id")
    fun pageSearch(pattern: String): PagingSource<Int, TitleEntity>

    @Query("SELECT * FROM titles WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): TitleEntity?
}
