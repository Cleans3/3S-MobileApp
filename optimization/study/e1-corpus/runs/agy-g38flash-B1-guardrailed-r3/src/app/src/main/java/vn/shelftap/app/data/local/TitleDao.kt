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
    fun getAllTitles(): PagingSource<Int, TitleEntity>

    // Rule 17: Query indexed Room @Fts4 table for text search and page results
    @Query("""
        SELECT titles.* FROM titles
        JOIN titles_fts ON titles.id = titles_fts.id
        WHERE titles_fts MATCH :query
        ORDER BY titles.title ASC
    """)
    fun searchTitlesFts(query: String): PagingSource<Int, TitleEntity>

    @Query("SELECT * FROM titles WHERE id = :id LIMIT 1")
    suspend fun getTitleById(id: String): TitleEntity?

    @Query("SELECT * FROM titles WHERE id = :id LIMIT 1")
    fun observeTitleById(id: String): Flow<TitleEntity?>

    @Query("SELECT COUNT(*) FROM titles")
    suspend fun getTitleCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTitles(titles: List<TitleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTitlesFts(ftsList: List<TitleFtsEntity>)

    @Query("DELETE FROM titles_fts WHERE id IN (:ids)")
    suspend fun deleteFtsByIds(ids: List<String>)

    @Transaction
    suspend fun upsertTitles(titles: List<TitleEntity>) {
        insertTitles(titles)
        val ids = titles.map { it.id }
        deleteFtsByIds(ids)
        val ftsList = titles.map { title ->
            TitleFtsEntity(
                id = title.id,
                isbn = title.isbn,
                title = title.title,
                authors = title.authors.joinToString(" "),
                subjects = title.subjects.joinToString(" ")
            )
        }
        insertTitlesFts(ftsList)
    }

    @Query("DELETE FROM titles")
    suspend fun clearAll()

    @Query("DELETE FROM titles_fts")
    suspend fun clearFts()

    @Transaction
    suspend fun clearDatabase() {
        clearAll()
        clearFts()
    }
}
