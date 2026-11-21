package com.library.catalogue.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT COUNT(*) FROM books")
    fun getBookCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCount(): Int

    @Query(
        """
        SELECT * FROM books
        WHERE (:query = '' OR title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' OR isbn LIKE '%' || :query || '%')
          AND (:genre = 'All' OR genre = :genre)
          AND (:onlyAvailable = 0 OR isAvailable = 1)
        ORDER BY id ASC
        """
    )
    fun searchBooksPaging(
        query: String,
        genre: String,
        onlyAvailable: Boolean
    ): PagingSource<Int, BookEntity>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getBookById(id: Long): BookEntity?

    @Query("SELECT DISTINCT genre FROM books ORDER BY genre ASC")
    fun getDistinctGenres(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(books: List<BookEntity>)

    @Query("DELETE FROM books")
    suspend fun clearAll()
}
