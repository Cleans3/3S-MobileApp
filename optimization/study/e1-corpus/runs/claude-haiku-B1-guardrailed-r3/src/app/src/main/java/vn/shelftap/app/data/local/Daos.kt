package vn.shelftap.app.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TitleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(titles: List<TitleEntity>)

    @Query("SELECT * FROM titles ORDER BY title, id")
    fun pageAll(): PagingSource<Int, TitleEntity>

    /** IN (subquery) instead of a join, so a title is returned once even if indexed twice. */
    @Query(
        "SELECT * FROM titles WHERE id IN " +
            "(SELECT titleId FROM title_search WHERE title_search MATCH :ftsQuery) " +
            "ORDER BY title, id"
    )
    fun searchPage(ftsQuery: String): PagingSource<Int, TitleEntity>

    @Query("SELECT * FROM titles WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): TitleEntity?
}

@Dao
interface TitleSearchDao {

    @Insert
    suspend fun insertAll(rows: List<TitleSearchEntity>)
}

@Dao
interface SyncStateDao {

    @Query("SELECT * FROM sync_state WHERE id = 0")
    suspend fun get(): SyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(state: SyncStateEntity)
}
