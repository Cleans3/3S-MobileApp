package vn.shelftap.app.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TitleDao {
    @Query("SELECT * FROM titles ORDER BY title COLLATE NOCASE ASC")
    fun getAllTitlesPaged(): PagingSource<Int, TitleEntity>

    @Query("""
        SELECT titles.* FROM titles
        JOIN titles_fts ON titles.rowid = titles_fts.rowid
        WHERE titles_fts MATCH :ftsQuery
        ORDER BY titles.title COLLATE NOCASE ASC
    """)
    fun searchTitlesFtsPaged(ftsQuery: String): PagingSource<Int, TitleEntity>

    @Query("SELECT * FROM titles WHERE id = :id LIMIT 1")
    suspend fun getTitleById(id: String): TitleEntity?

    @Query("SELECT COUNT(*) FROM titles")
    suspend fun getTitleCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTitles(titles: List<TitleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTitle(title: TitleEntity)

    @Query("DELETE FROM titles")
    suspend fun clearAll()
}
