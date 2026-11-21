package vn.shelftap.app

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CatalogueRepository(private val db: CatalogueDb, private val api: TitleApi, private val io: CoroutineDispatcher = Dispatchers.IO) {
    suspend fun cached(query: String): List<Title> = withContext(io) { if (query.isBlank()) db.all() else db.find(query) }
    suspend fun refresh(query: String): Unit = withContext(io) {
        val pages = mutableListOf<Title>(); var page = 0
        do { val current = api.fetchPage(query, page++, 50); pages += current } while (current.size == 50)
        if (pages.isNotEmpty()) db.replace(pages)
    }
    suspend fun detail(id: String): Title? = withContext(io) { db.all().firstOrNull { it.id == id } ?: runCatching { api.fetch(id) }.getOrNull() }
}
