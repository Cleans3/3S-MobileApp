package vn.shelftap.app.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CatalogueRepository(private val dao: TitleDao, private val api: CatalogueApi, private val io: CoroutineDispatcher = Dispatchers.IO) {
    fun observe(query: String): Flow<List<TitleEntity>> = if (query.isBlank()) dao.observeAll() else dao.observeSearch(query.trim().split(Regex("\\s+")).joinToString(" ") { "${it.replace("\"", "")}*" })
    suspend fun get(id: String): Title? = withContext(io) { dao.find(id)?.asDomain() ?: runCatching { api.title(id) }.getOrNull() }
    suspend fun syncIfNeeded() = withContext(io) {
        if (dao.count() > 0) return@withContext
        var page = 0; var total = Int.MAX_VALUE
        while (page * 50 < total) {
            val result = api.page("", page, 50); total = result.total
            if (result.items.isEmpty()) break
            dao.insertAll(result.items.map(TitleEntity::from)); page++
        }
    }
}
