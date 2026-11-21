package com.library.catalogue.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.library.catalogue.data.generator.BookDataSeeder
import com.library.catalogue.data.local.LibraryDatabase
import com.library.catalogue.data.model.Book
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookRepositoryImpl(
    private val database: LibraryDatabase,
    private val seeder: BookDataSeeder
) : BookRepository {

    override fun getBooks(
        query: String,
        genre: String,
        onlyAvailable: Boolean
    ): Flow<PagingData<Book>> {
        val trimmedQuery = query.trim()
        return Pager(
            config = PagingConfig(
                pageSize = 30,
                prefetchDistance = 15,
                enablePlaceholders = true,
                maxSize = 200
            ),
            pagingSourceFactory = {
                database.bookDao().searchBooksPaging(
                    query = trimmedQuery,
                    genre = genre,
                    onlyAvailable = onlyAvailable
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel() }
        }
    }

    override fun getTotalBookCount(): Flow<Int> {
        return database.bookDao().getBookCountFlow()
    }

    override fun getGenres(): Flow<List<String>> {
        return database.bookDao().getDistinctGenres()
    }

    override suspend fun getBookById(id: Long): Book? {
        return database.bookDao().getBookById(id)?.toDomainModel()
    }

    override val seedingProgress: Flow<Float>
        get() = seeder.seedingProgress

    override suspend fun ensureDataSeeded() {
        seeder.seedDatabaseIfNeeded(100_000)
    }
}
