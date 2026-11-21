package vn.shelftap.app.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CatalogueRepository(
    private val database: CatalogueDatabase,
    private val api: CatalogueApi,
    private val io: CoroutineDispatcher
) {
    private val dao = database.catalogueDao()
    private val pageSize = 50

    fun catalogue(): Flow<PagingData<TitleEntity>> = Pager(PagingConfig(pageSize = pageSize, enablePlaceholders = false)) { dao.catalogue() }.flow

    fun search(query: String): Flow<PagingData<TitleEntity>> = Pager(
        PagingConfig(pageSize = pageSize, enablePlaceholders = false)
    ) { dao.search(ftsQuery(query)) }.flow

    fun observeTitle(id: String): Flow<TitleEntity?> = dao.observeTitle(id)

    suspend fun loadPage(page: Int, query: String = ""): Boolean = withContext(io) {
        runCatching {
            val result = api.titles(query, page, pageSize)
            database.withTransaction { dao.upsertAll(result.items.map(TitleDto::toEntity)) }
            result.items.isNotEmpty() && (page + 1) * pageSize < result.total
        }.getOrDefault(false)
    }

    suspend fun loadTitle(id: String) = withContext(io) {
        runCatching { database.catalogueDao().upsertAll(listOf(api.title(id).toEntity())) }
    }

    suspend fun cachedCount(): Int = withContext(io) { dao.count() }

    private fun ftsQuery(input: String): String = input.trim().split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(" AND ") { "\"${it.replace(Regex("[\"*]"), "") }*\"" }
}
