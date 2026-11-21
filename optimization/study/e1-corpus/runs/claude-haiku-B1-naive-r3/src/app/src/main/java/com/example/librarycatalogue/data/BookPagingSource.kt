package com.example.librarycatalogue.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import java.io.IOException
import retrofit2.HttpException

/**
 * Loads one page of books at a time from the API. Page numbers are 1-based and
 * every request asks for exactly [PAGE_SIZE] items so page boundaries stay stable.
 */
internal class BookPagingSource(
    private val api: LibraryApi,
    private val query: String?,
) : PagingSource<Int, Book>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Book> {
        val page = params.key ?: STARTING_PAGE
        return try {
            val books = api
                .searchBooks(query = query?.takeIf { it.isNotBlank() }, page = page, perPage = PAGE_SIZE)
                .map { it.toDomain() }

            LoadResult.Page(
                data = books,
                prevKey = if (page == STARTING_PAGE) null else page - 1,
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

    companion object {
        const val STARTING_PAGE = 1
        const val PAGE_SIZE = 30
    }
}
