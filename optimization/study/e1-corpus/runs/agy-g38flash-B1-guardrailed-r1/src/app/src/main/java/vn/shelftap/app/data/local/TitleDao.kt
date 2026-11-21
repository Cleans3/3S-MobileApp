package vn.shelftap.app.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TitleDao {
    @Query("SELECT * FROM titles ORDER BY title ASC")
    fun getTitlesPaged(): PagingSource<Int, TitleEntity>

    @Query("""
        SELECT * FROM titles 
        WHERE id IN (
            SELECT titleId FROM titles_fts 
            WHERE titles_fts MATCH :ftsQuery
        ) 
        ORDER BY title ASC
    """)
    fun searchTitlesFtsPaged(ftsQuery: String): PagingSource<Int, TitleEntity>

    @Query("SELECT * FROM titles WHERE id = :id")
    suspend fun getTitleById(id: String): TitleEntity?

    @Query("SELECT * FROM titles WHERE id = :id")
    fun observeTitleById(id: String): Flow<TitleEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTitles(titles: List<TitleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFts(ftsItems: List<TitleFtsEntity>)

    @Query("DELETE FROM titles_fts WHERE titleId IN (:titleIds)")
    suspend fun deleteFtsByIds(titleIds: List<String>)

    @Transaction
    suspend fun upsertTitlesWithFts(titles: List<TitleEntity>) {
        insertTitles(titles)
        val ids = titles.map { it.id }
        deleteFtsByIds(ids)
        insertFts(titles.map { title ->
            TitleFtsEntity(
                titleId = title.id,
                title = title.title,
                authors = title.authors.joinToString(" "),
                isbn = title.isbn,
                subjects = title.subjects.joinToString(" ")
            )
        })
    }

    @Query("SELECT COUNT(*) FROM titles")
    suspend fun getTitlesCount(): Int

    @Query("DELETE FROM titles")
    suspend fun clearTitles()

    @Query("DELETE FROM titles_fts")
    suspend fun clearFts()
}
