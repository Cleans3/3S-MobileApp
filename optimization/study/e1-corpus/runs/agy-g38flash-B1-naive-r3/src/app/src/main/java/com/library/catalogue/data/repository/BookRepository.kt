package com.library.catalogue.data.repository

import androidx.paging.PagingData
import com.library.catalogue.data.model.Book
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    fun getBooks(
        query: String,
        genre: String,
        onlyAvailable: Boolean
    ): Flow<PagingData<Book>>

    fun getTotalBookCount(): Flow<Int>

    fun getGenres(): Flow<List<String>>

    suspend fun getBookById(id: Long): Book?

    val seedingProgress: Flow<Float>

    suspend fun ensureDataSeeded()
}
