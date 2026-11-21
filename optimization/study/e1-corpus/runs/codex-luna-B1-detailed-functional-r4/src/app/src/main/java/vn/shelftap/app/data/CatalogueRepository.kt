package vn.shelftap.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.shelftap.app.model.Title

class CatalogueRepository(context: Context) {
    private val db = TitleDatabase(context.applicationContext)
    private val api = CatalogueApi()
    suspend fun local(query: String) = withContext(Dispatchers.IO) { db.find(query) }
    suspend fun detail(id: String) = withContext(Dispatchers.IO) { db.get(id) }
    suspend fun refresh(onProgress: (Int, Int) -> Unit) = withContext(Dispatchers.IO) {
        var page = 0; var total = Int.MAX_VALUE; var loaded = 0
        while (loaded < total) {
            val result = api.page(page = page)
            db.upsert(result.items); loaded += result.items.size; total = result.total
            onProgress(loaded, total); page++
            if (result.items.isEmpty()) break
        }
    }
}
