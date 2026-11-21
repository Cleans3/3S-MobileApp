package vn.shelftap.app.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
abstract class CatalogueDao {

    @Query("SELECT * FROM titles ORDER BY title COLLATE NOCASE, id")
    abstract fun pageCatalogue(): PagingSource<Int, TitleEntity>

    /** [match] is an FTS4 MATCH expression built by ftsMatchExpression(). */
    @Query(
        "SELECT titles.* FROM titles " +
            "INNER JOIN title_search ON title_search.titleId = titles.id " +
            "WHERE title_search MATCH :match " +
            "ORDER BY titles.title COLLATE NOCASE, titles.id"
    )
    abstract fun pageSearch(match: String): PagingSource<Int, TitleEntity>

    @Query("SELECT * FROM titles WHERE id = :id LIMIT 1")
    abstract suspend fun findById(id: String): TitleEntity?

    @Query("SELECT * FROM sync_state WHERE id = 1")
    abstract suspend fun syncState(): SyncStateEntity?

    /** Returns the new row id per item, or -1 where the title already existed. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertTitlesIgnoring(items: List<TitleEntity>): List<Long>

    @Update
    abstract suspend fun updateTitles(items: List<TitleEntity>)

    @Insert
    abstract suspend fun insertSearchRows(items: List<TitleSearchEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun saveSyncState(state: SyncStateEntity)

    /**
     * Inserts new titles and their search rows, and updates titles already stored.
     * Search rows are written only once per title, so re-running a download never duplicates them.
     */
    @Transaction
    open suspend fun upsertTitles(items: List<TitleEntity>) {
        if (items.isEmpty()) return
        val rowIds = insertTitlesIgnoring(items)
        val existing = mutableListOf<TitleEntity>()
        val added = mutableListOf<TitleSearchEntity>()
        for ((index, item) in items.withIndex()) {
            if (rowIds[index] == -1L) {
                existing.add(item)
            } else {
                added.add(item.toSearchRow())
            }
        }
        if (existing.isNotEmpty()) updateTitles(existing)
        if (added.isNotEmpty()) insertSearchRows(added)
    }

    /** Stores one downloaded page and the download progress in the same transaction. */
    @Transaction
    open suspend fun upsertPage(items: List<TitleEntity>, state: SyncStateEntity) {
        upsertTitles(items)
        saveSyncState(state)
    }
}
