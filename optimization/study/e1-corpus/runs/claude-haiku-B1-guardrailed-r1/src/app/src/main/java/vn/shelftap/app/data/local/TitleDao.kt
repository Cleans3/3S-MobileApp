package vn.shelftap.app.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TitleDao {

    @Upsert
    suspend fun upsertTitles(titles: List<TitleEntity>)

    // The FTS rowid equals the title rowid, so these statements use rowid lookups, not scans.
    @Query("DELETE FROM title_fts WHERE rowid IN (SELECT rowid FROM title WHERE id IN (:ids))")
    suspend fun deleteSearchRows(ids: List<String>)

    @Query(
        "INSERT INTO title_fts (rowid, docId, titleText, authorsText, isbn, subjectsText) " +
            "SELECT rowid, id, title, authors, isbn, subjects FROM title WHERE id IN (:ids)"
    )
    suspend fun insertSearchRows(ids: List<String>)

    @Query("SELECT * FROM title ORDER BY sort_title, id")
    fun pagingCatalogue(): PagingSource<Int, TitleEntity>

    @Query(
        "SELECT title.* FROM title " +
            "JOIN title_fts ON title_fts.docId = title.id " +
            "WHERE title_fts MATCH :match " +
            "ORDER BY title.sort_title, title.id"
    )
    fun pagingSearch(match: String): PagingSource<Int, TitleEntity>

    @Query("SELECT * FROM title WHERE id = :id")
    fun observeById(id: String): Flow<TitleEntity?>

    @Query("SELECT * FROM sync_state WHERE id = 1")
    suspend fun getSyncState(): SyncStateEntity?

    @Upsert
    suspend fun saveSyncState(state: SyncStateEntity)
}
