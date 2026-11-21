package vn.shelftap.app.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.map
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import vn.shelftap.app.data.local.AppDatabase
import vn.shelftap.app.data.local.TitleEntity
import vn.shelftap.app.data.remote.CatalogueApi
import vn.shelftap.app.model.Title

private val NON_TOKEN_CHARS = Regex("[^\\p{L}\\p{N}]+")

/**
 * Turns the typed text into an FTS4 MATCH expression: every word becomes a quoted prefix
 * term, and the terms are ANDed. Quoting stops words such as "or" or "not" being read as operators.
 * Returns null when nothing searchable is left.
 */
internal fun buildFtsQuery(rawQuery: String): String? {
    val tokens = rawQuery.split(NON_TOKEN_CHARS).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return null
    return tokens.joinToString(separator = " ") { "\"$it\"*" }
}

class TitleRepository(
    private val database: AppDatabase,
    private val api: CatalogueApi,
    private val ioDispatcher: CoroutineDispatcher,
) {
    /** All titles from the local store, paged by 50. Works with no connection. */
    fun pageAll(): Flow<PagingData<Title>> = pager { database.titleDao().pageAll() }

    /** Titles whose title, author, ISBN or subject matches the text, from the local FTS index. */
    fun search(rawQuery: String): Flow<PagingData<Title>> {
        val ftsQuery = buildFtsQuery(rawQuery) ?: return pageAll()
        return pager { database.titleDao().searchPage(ftsQuery) }
    }

    /** Local copy first; the API is asked only when the title was never downloaded. */
    suspend fun getTitle(id: String): Title? = withContext(ioDispatcher) {
        database.titleDao().byId(id)?.toDomain() ?: api.findById(id)
    }

    private fun pager(source: () -> PagingSource<Int, TitleEntity>): Flow<PagingData<Title>> =
        Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = source,
        )
            .flow
            .map { data -> data.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    private companion object {
        const val PAGE_SIZE = 50
    }
}
