package com.example.libcatalog.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

class BookRepository(private val api: BookApi) {

    /**
     * Paged stream of books matching [query] (blank means the whole catalogue).
     * Each call creates a new Pager, so changing the query starts a fresh list.
     */
    fun search(query: String): Flow<PagingData<Book>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            initialLoadSize = PAGE_SIZE,
            prefetchDistance = PAGE_SIZE / 2,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { BookPagingSource(api, query.takeIf { it.isNotBlank() }) },
    ).flow
}
