package vn.shelftap.app.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CatalogueRepository(
    private val database: CatalogueDatabase,
    private val api: CatalogueApi = CatalogueApi(),
    private val io: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun page(query: String, page: Int, size: Int): List<Title> = withContext(io) {
        val local = database.page(query, page, size)
        if (local.size >= size) return@withContext local
        runCatching { api.titles(query, page, size).also(database::upsert) }.getOrElse { local }
    }

    suspend fun detail(id: String): Title? = withContext(io) {
        database.byId(id) ?: runCatching { api.title(id).also { database.upsert(listOf(it)) } }.getOrNull()
    }
}
