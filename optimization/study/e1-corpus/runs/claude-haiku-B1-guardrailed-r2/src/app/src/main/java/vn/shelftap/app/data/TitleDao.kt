package vn.shelftap.app.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface TitleDao {

    @Query("SELECT * FROM titles ORDER BY sortTitle, id")
    fun pagingAll(): PagingSource<Int, TitleEntity>

    /** [match] is an FTS4 MATCH expression built by [FtsQuery]. */
    @Query(
        "SELECT titles.* FROM titles " +
            "INNER JOIN title_search ON title_search.id = titles.id " +
            "WHERE title_search MATCH :match " +
            "ORDER BY titles.sortTitle, titles.id"
    )
    fun pagingSearch(match: String): PagingSource<Int, TitleEntity>

    @Query("SELECT * FROM titles WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): TitleEntity?

    @Query("SELECT * FROM sync_state WHERE id = 1")
    suspend fun syncState(): SyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTitles(titles: List<TitleEntity>)

    @Insert
    suspend fun insertSearchRows(rows: List<TitleSearchEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSyncState(state: SyncStateEntity)

    @Query("DELETE FROM titles")
    suspend fun clearTitles()

    @Query("DELETE FROM title_search")
    suspend fun clearSearchRows()

    /** Writes one downloaded page and its progress together, so a crash never leaves them out of step. */
    @Transaction
    suspend fun savePage(
        titles: List<TitleEntity>,
        searchRows: List<TitleSearchEntity>,
        state: SyncStateEntity
    ) {
        insertTitles(titles)
        insertSearchRows(searchRows)
        saveSyncState(state)
    }

    /** Starts a fresh download: empties the catalogue and resets progress. */
    @Transaction
    suspend fun resetCatalogue() {
        clearTitles()
        clearSearchRows()
        saveSyncState(SyncStateEntity(nextPage = 0, total = 0, complete = false))
    }
}
