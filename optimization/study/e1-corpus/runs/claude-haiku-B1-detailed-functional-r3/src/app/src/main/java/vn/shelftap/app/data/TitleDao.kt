package vn.shelftap.app.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TitleDao {

    @Query("SELECT * FROM titles ORDER BY title COLLATE NOCASE, id")
    fun pagingAll(): PagingSource<Int, TitleEntity>

    /** [pattern] must already be lower-cased and LIKE-escaped. */
    @Query("SELECT * FROM titles WHERE searchText LIKE '%' || :pattern || '%' ESCAPE '\\' ORDER BY title COLLATE NOCASE, id")
    fun pagingSearch(pattern: String): PagingSource<Int, TitleEntity>

    @Query("SELECT * FROM titles WHERE id = :id")
    suspend fun getById(id: String): TitleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: List<TitleEntity>)

    @Query("DELETE FROM titles WHERE syncedAt < :before")
    suspend fun deleteNotSyncedSince(before: Long)
}
