package com.example.librarycatalogue.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

class BookRepository internal constructor(private val api: LibraryApi) {

    /** Paged stream of books matching [query]; an empty query returns the whole catalogue. */
    fun searchBooks(query: String): Flow<PagingData<Book>> = Pager(
        config = PagingConfig(
            pageSize = BookPagingSource.PAGE_SIZE,
            initialLoadSize = BookPagingSource.PAGE_SIZE,
            prefetchDistance = 10,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { BookPagingSource(api, query) },
    ).flow
}
