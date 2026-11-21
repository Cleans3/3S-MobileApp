package vn.shelftap.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CatalogueRepository(context: Context) {
    private val db = CatalogueDb(context.applicationContext); private val api = ApiClient()
    suspend fun local(query: String): List<Title> = withContext(Dispatchers.IO) { if (query.isBlank()) db.all() else db.search(query) }
    suspend fun localById(id: String): Title? = withContext(Dispatchers.IO) { db.get(id) }
    suspend fun refreshAll() = withContext(Dispatchers.IO) { var page = 0; var total = Int.MAX_VALUE; do { val p = api.page("", page, 50); db.upsert(p.items); total = p.total; page++ } while (page * 50 < total && p.items.isNotEmpty()) }
    suspend fun fetchOne(id: String): Title = withContext(Dispatchers.IO) { api.title(id).also { db.upsert(listOf(it)) } }
}
