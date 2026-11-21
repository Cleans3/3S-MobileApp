package vn.shelftap.app.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for querying and caching titles locally.
 * Supports Paging 3 for smooth scrolling through 100,000 titles,
 * as well as offline text searching matching title, author, ISBN, or subject.
 */
@Dao
interface TitleDao {

    /**
     * Paged query of all titles ordered alphabetically by title.
     */
    @Query("SELECT * FROM titles ORDER BY title COLLATE NOCASE ASC")
    fun getAllTitlesPaged(): PagingSource<Int, TitleEntity>

    /**
     * Search query matching title, ISBN, author, or subject (case-insensitive).
     */
    @Query("""
        SELECT * FROM titles 
        WHERE title LIKE '%' || :query || '%'
           OR isbn LIKE '%' || :query || '%'
           OR authorsSearch LIKE '%' || :lowerQuery || '%'
           OR subjectsSearch LIKE '%' || :lowerQuery || '%'
        ORDER BY title COLLATE NOCASE ASC
    """)
    fun searchTitlesPaged(query: String, lowerQuery: String = query.lowercase()): PagingSource<Int, TitleEntity>

    /**
     * Get a single title by its unique ID.
     */
    @Query("SELECT * FROM titles WHERE id = :id LIMIT 1")
    suspend fun getTitleById(id: String): TitleEntity?

    /**
     * Observable Flow of a single title by its unique ID.
     */
    @Query("SELECT * FROM titles WHERE id = :id LIMIT 1")
    fun getTitleByIdFlow(id: String): Flow<TitleEntity?>

    /**
     * Count the total number of cached titles.
     */
    @Query("SELECT COUNT(*) FROM titles")
    suspend fun getTitleCount(): Int

    /**
     * Observable Flow for the total count of cached titles.
     */
    @Query("SELECT COUNT(*) FROM titles")
    fun getTitleCountFlow(): Flow<Int>

    /**
     * Insert or update titles in batch.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTitles(titles: List<TitleEntity>)

    /**
     * Delete all titles in the local cache.
     */
    @Query("DELETE FROM titles")
    suspend fun clearAll()
}
