package com.example.librarycatalogue.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.CancellationException

const val PAGE_SIZE = 30

/**
 * Loads one page of books from the API. Page numbers are 1-based.
 * A blank query returns the whole catalogue, paged.
 */
class BookPagingSource(
    private val api: LibraryApi,
    private val query: String,
) : PagingSource<Int, Book>() {

    override fun getRefreshKey(state: PagingState<Int, Book>): Int? {
        return state.anchorPosition?.let { anchor ->
            val page = state.closestPageToPosition(anchor)
            page?.prevKey?.plus(1) ?: page?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Book> {
        val page = params.key ?: 1
        return try {
            val response = api.searchBooks(
                page = page,
                perPage = PAGE_SIZE,
                query = query.takeIf { it.isNotBlank() },
            )
            LoadResult.Page(
                data = response.items.map { it.toDomain() },
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (page >= response.totalPages || response.items.isEmpty()) null else page + 1,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
