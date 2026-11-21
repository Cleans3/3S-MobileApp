package com.example.libcatalog.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import retrofit2.HttpException
import java.io.IOException

/** Number of books requested per page. Fixed so page numbers line up with server pages. */
const val PAGE_SIZE = 30

private const val FIRST_PAGE = 1

/**
 * Loads one page of catalogue results at a time. Only the pages currently near the
 * screen are kept in memory, so a 100,000-book catalogue is never loaded at once.
 */
class BookPagingSource(
    private val api: BookApi,
    private val query: String?,
) : PagingSource<Int, Book>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Book> {
        val page = params.key ?: FIRST_PAGE
        return try {
            val books = api.searchBooks(query, page, PAGE_SIZE)
            LoadResult.Page(
                data = books,
                prevKey = if (page == FIRST_PAGE) null else page - 1,
                nextKey = if (books.size < PAGE_SIZE) null else page + 1,
            )
        } catch (e: IOException) {
            LoadResult.Error(e)
        } catch (e: HttpException) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Book>): Int? {
        val anchor = state.anchorPosition ?: return null
        val page = state.closestPageToPosition(anchor) ?: return null
        return page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
    }
}
