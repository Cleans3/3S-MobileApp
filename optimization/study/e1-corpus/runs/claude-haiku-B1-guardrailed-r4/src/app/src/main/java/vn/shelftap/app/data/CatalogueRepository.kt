package vn.shelftap.app.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import vn.shelftap.app.data.local.CatalogueDao
import vn.shelftap.app.data.local.TitleEntity
import vn.shelftap.app.data.local.toEntity
import vn.shelftap.app.data.local.toTitle
import vn.shelftap.app.data.remote.CatalogueApi
import vn.shelftap.app.model.Title

private const val LIST_PAGE_SIZE = 40
private val NON_TOKEN_CHARS = Regex("[^\\p{L}\\p{N}]+")

/**
 * Builds an FTS4 MATCH expression for [query]: each word becomes a prefix term and
 * the terms are ANDed. Words are lowercased so that FTS operators (AND, OR, NOT)
 * are never interpreted. Returns null when the query has no letters or digits.
 */
internal fun ftsMatchExpression(query: String): String? {
    val tokens = query.lowercase().split(NON_TOKEN_CHARS).filter { it.isNotEmpty() }
    return if (tokens.isEmpty()) null else tokens.joinToString(separator = " ") { "$it*" }
}

/**
 * Offline-first data access. The local database is the source of truth for lists,
 * search and detail; the network is used only to download the catalogue and to
 * fetch a title that is not stored yet.
 */
class CatalogueRepository(
    private val dao: CatalogueDao,
    private val api: CatalogueApi,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {

    /** All titles, ordered by title. */
    fun catalogue(): Flow<PagingData<Title>> = pager { dao.pageCatalogue() }

    /** Titles whose title, authors, ISBN or subject match [query]. Blank query shows everything. */
    fun search(query: String): Flow<PagingData<Title>> {
        if (query.isBlank()) return catalogue()
        val match = ftsMatchExpression(query) ?: return flowOf(PagingData.empty<Title>())
        return pager { dao.pageSearch(match) }
    }

    /** Local copy first; falls back to the API and stores the result. Null if the title does not exist. */
    suspend fun title(id: String): Title? = withContext(io) {
        val local = dao.findById(id)
        if (local != null) return@withContext local.toTitle()
        val remote = api.fetchTitle(id) ?: return@withContext null
        dao.upsertTitles(listOf(remote.toEntity()))
        remote
    }

    private fun pager(source: () -> PagingSource<Int, TitleEntity>): Flow<PagingData<Title>> =
        Pager(
            config = PagingConfig(pageSize = LIST_PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = source,
        ).flow.map { paging -> paging.map { entity -> entity.toTitle() } }
}
