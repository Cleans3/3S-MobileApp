package com.example.librarycatalogue.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

class BookRepository(private val api: LibraryApi) {

    /**
     * Returns a paged stream of books matching [query] (all books when blank).
     * Only the pages currently needed are held in memory.
     */
    fun searchBooks(query: String): Flow<PagingData<Book>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            initialLoadSize = PAGE_SIZE,
            prefetchDistance = PAGE_SIZE / 2,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { BookPagingSource(api, query) },
    ).flow
}
