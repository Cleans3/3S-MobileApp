package vn.shelftap.slop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray

/** Loads the catalog from the ShelfTap API. */
class CatalogRepository(private val baseUrl: String = "http://10.0.2.2:8080/") {
    suspend fun load(): List<Book> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(baseUrl + "api/books").build()
        Net.client.newCall(request).execute().use { response ->
            val array = JSONArray(response.body.string())
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                Book(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    author = o.getString("author"),
                    coverUrl = o.getString("coverUrl"),
                    tags = List(o.getJSONArray("tags").length()) { j -> o.getJSONArray("tags").getString(j) },
                )
            }
        }
    }
}
