package com.example.libcatalogue.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.libcatalogue.data.remote.BookApi
import kotlinx.coroutines.flow.Flow

class BookRepository(private val api: BookApi) {

    /**
     * Paged stream of books matching [query]. An empty query returns the whole catalogue.
     * Each new query creates a fresh Pager, so results always start from page 1.
     */
    fun books(query: String): Flow<PagingData<Book>> {
        return Pager(
            config = PagingConfig(
                pageSize = BookPagingSource.PAGE_SIZE,
                prefetchDistance = BookPagingSource.PAGE_SIZE / 2,
                initialLoadSize = BookPagingSource.PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { BookPagingSource(api, query) }
        ).flow
    }
}
