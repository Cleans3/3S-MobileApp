package com.example.librarycatalogue.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY id ASC")
    fun getAllBooksPaged(): PagingSource<Int, BookEntity>

    @Query("""
        SELECT * FROM books 
        WHERE (title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' OR isbn LIKE '%' || :query || '%')
        ORDER BY id ASC
    """)
    fun searchBooksPaged(query: String): PagingSource<Int, BookEntity>

    @Query("SELECT * FROM books WHERE genre = :genre ORDER BY id ASC")
    fun getBooksByGenrePaged(genre: String): PagingSource<Int, BookEntity>

    @Query("""
        SELECT * FROM books 
        WHERE genre = :genre 
          AND (title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' OR isbn LIKE '%' || :query || '%')
        ORDER BY id ASC
    """)
    fun searchBooksWithGenrePaged(query: String, genre: String): PagingSource<Int, BookEntity>

    @Query("SELECT COUNT(*) FROM books")
    fun getBookCount(): Int

    @Query("SELECT COUNT(*) FROM books")
    fun getBookCountFlow(): Flow<Int>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    fun getBookById(id: Long): Flow<BookEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(books: List<BookEntity>)

    @Query("UPDATE books SET isAvailable = :available WHERE id = :id")
    suspend fun updateAvailability(id: Long, available: Boolean)
}
