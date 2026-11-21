package com.example.libcatalogue.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.libcatalogue.data.remote.BookApi
import com.example.libcatalogue.data.remote.BookDto
import retrofit2.HttpException
import java.io.IOException

/**
 * Loads catalogue pages from the API, one page at a time.
 * Only the pages the user has scrolled near are ever requested, so the
 * ~100,000-book catalogue is never loaded into memory at once.
 */
class BookPagingSource(
    private val api: BookApi,
    private val query: String?
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
                query = query?.takeIf { it.isNotBlank() },
                page = page,
                size = PAGE_SIZE
            )
            val books = response.items.orEmpty().mapNotNull { it.toDomain() }
            LoadResult.Page(
                data = books,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (response.items.isNullOrEmpty()) null else page + 1
            )
        } catch (e: IOException) {
            LoadResult.Error(e)
        } catch (e: HttpException) {
            LoadResult.Error(e)
        }
    }

    private fun BookDto.toDomain(): Book? {
        val bookId = id ?: return null
        return Book(
            id = bookId,
            title = title?.takeIf { it.isNotBlank() } ?: "Untitled",
            authors = authors.orEmpty(),
            year = year,
            coverUrl = coverUrl?.takeIf { it.isNotBlank() },
            available = available ?: false
        )
    }

    companion object {
        const val PAGE_SIZE = 30
    }
}
