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
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTitles(titles: List<TitleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFtsTitles(ftsTitles: List<TitleFtsEntity>)

    @Transaction
    suspend fun insertTitlesWithFts(titles: List<TitleEntity>) {
        insertTitles(titles)
        val ftsList = titles.map { title ->
            TitleFtsEntity(
                id = title.id,
                isbn = title.isbn,
                title = title.title,
                authors = title.authors.joinToString(" "),
                subjects = title.subjects.joinToString(" ")
            )
        }
        insertFtsTitles(ftsList)
    }

    @Query("SELECT * FROM titles ORDER BY title ASC")
    fun getAllTitles(): PagingSource<Int, TitleEntity>

    @Query("""
        SELECT titles.* FROM titles
        JOIN titles_fts ON titles.id = titles_fts.id
        WHERE titles_fts MATCH :searchQuery
        ORDER BY titles.title ASC
    """)
    fun searchTitlesFts(searchQuery: String): PagingSource<Int, TitleEntity>

    @Query("SELECT * FROM titles WHERE id = :id LIMIT 1")
    suspend fun getTitleById(id: String): TitleEntity?

    @Query("SELECT * FROM titles WHERE id = :id LIMIT 1")
    fun observeTitleById(id: String): Flow<TitleEntity?>

    @Query("SELECT COUNT(*) FROM titles")
    suspend fun getTitleCount(): Int

    @Query("DELETE FROM titles")
    suspend fun clearTitles()

    @Query("DELETE FROM titles_fts")
    suspend fun clearFtsTitles()

    @Transaction
    suspend fun clearAll() {
        clearTitles()
        clearFtsTitles()
    }
}
