package vn.shelftap.app.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TitleDao {

    @Query("SELECT * FROM titles ORDER BY title COLLATE NOCASE, id")
    fun pagingAll(): PagingSource<Int, TitleEntity>

    /** [pattern] is already lowercased and escaped with '!' (see CatalogueRepository.likePattern). */
    @Query(
        "SELECT * FROM titles WHERE searchText LIKE '%' || :pattern || '%' ESCAPE '!' " +
            "ORDER BY title COLLATE NOCASE, id"
    )
    fun pagingSearch(pattern: String): PagingSource<Int, TitleEntity>

    @Query("SELECT * FROM titles WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<TitleEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TitleEntity>)

    @Query("DELETE FROM titles")
    suspend fun deleteAll()
}
