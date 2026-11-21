package vn.shelftap.app.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TitleEntity>)

    @Query("SELECT * FROM titles ORDER BY title COLLATE NOCASE")
    fun catalogue(): PagingSource<Int, TitleEntity>

    @Query("SELECT * FROM titles WHERE id = :id")
    fun observeTitle(id: String): Flow<TitleEntity?>

    @Query("SELECT titles.* FROM titles JOIN titles_fts ON titles.rowid = titles_fts.rowid WHERE titles_fts MATCH :match ORDER BY titles.title COLLATE NOCASE")
    fun search(match: String): PagingSource<Int, TitleEntity>

    @Query("SELECT COUNT(*) FROM titles")
    suspend fun count(): Int
}
