package com.example.librarycatalogue.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.librarycatalogue.data.local.BookDao
import com.example.librarycatalogue.data.local.BookDatabaseSeeder
import com.example.librarycatalogue.data.local.BookEntity
import com.example.librarycatalogue.data.local.LibraryDatabase
import com.example.librarycatalogue.model.Book
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class BookRepository(
    private val database: LibraryDatabase
) {
    private val bookDao: BookDao = database.bookDao()

    private val _isSeeding = MutableStateFlow(false)
    val isSeeding: StateFlow<Boolean> = _isSeeding.asStateFlow()

    private val _seedingProgress = MutableStateFlow(0f)
    val seedingProgress: StateFlow<Float> = _seedingProgress.asStateFlow()

    val totalBooksCount: Flow<Int> = bookDao.getBookCountFlow()

    suspend fun initializeCatalogue() {
        val count = bookDao.getBookCount()
        if (count < BookDatabaseSeeder.TARGET_BOOK_COUNT) {
            _isSeeding.value = true
            BookDatabaseSeeder.seedIfNeeded(database) { current, total ->
                _seedingProgress.value = current.toFloat() / total.toFloat()
            }
            _isSeeding.value = false
        }
    }

    fun getBooks(query: String, genre: String): Flow<PagingData<Book>> {
        val trimmedQuery = query.trim()
        val isFilteredByGenre = genre.isNotBlank() && genre != "All Genres"
        val hasSearchQuery = trimmedQuery.isNotBlank()

        val pagingSourceFactory = {
            when {
                hasSearchQuery && isFilteredByGenre -> {
                    bookDao.searchBooksWithGenrePaged(trimmedQuery, genre)
                }
                hasSearchQuery -> {
                    bookDao.searchBooksPaged(trimmedQuery)
                }
                isFilteredByGenre -> {
                    bookDao.getBooksByGenrePaged(genre)
                }
                else -> {
                    bookDao.getAllBooksPaged()
                }
            }
        }

        return Pager(
            config = PagingConfig(
                pageSize = 40,
                prefetchDistance = 20,
                enablePlaceholders = true,
                initialLoadSize = 60,
                maxSize = 200
            ),
            pagingSourceFactory = pagingSourceFactory
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel() }
        }
    }

    suspend fun toggleAvailability(bookId: Long, currentAvailability: Boolean) {
        bookDao.updateAvailability(bookId, !currentAvailability)
    }

    private fun BookEntity.toDomainModel(): Book {
        return Book(
            id = id,
            title = title,
            author = author,
            isbn = isbn,
            genre = genre,
            publicationYear = publicationYear,
            pageCount = pageCount,
            description = description,
            shelfLocation = shelfLocation,
            isAvailable = isAvailable,
            coverColorSeed = coverColorSeed,
            coverImageUrl = coverImageUrl
        )
    }
}
