package vn.shelftap.app.data.remote

import org.json.JSONArray
import org.json.JSONObject
import vn.shelftap.app.model.Title
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Client for the ShelfTap REST API. Blocking calls: run them on Dispatchers.IO. */
class TitleApi(private val baseUrl: String = BASE_URL) {

    class Page(
        val items: List<Title>,
        val page: Int,
        val size: Int,
        val total: Int,
    )

    /** GET /api/titles?page=&size= (no query text, so the whole catalogue is returned page by page). */
    fun fetchPage(page: Int, size: Int = PAGE_SIZE): Page {
        val json = JSONObject(get("$baseUrl/api/titles?page=$page&size=$size"))
        val items = json.getJSONArray("items")
        return Page(
            items = List(items.length()) { parseTitle(items.getJSONObject(it)) },
            page = json.optInt("page", page),
            size = json.optInt("size", size),
            total = json.optInt("total", 0),
        )
    }

    /** GET /api/titles/{id} */
    fun fetchTitle(id: String): Title {
        val encodedId = URLEncoder.encode(id, "UTF-8").replace("+", "%20")
        return parseTitle(JSONObject(get("$baseUrl/api/titles/$encodedId")))
    }

    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Accept", "application/json")
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val BASE_URL = "https://api.shelftap.example"
        const val PAGE_SIZE = 50
    }
}

internal fun parseTitle(o: JSONObject): Title = Title(
    id = o.getString("id"),
    isbn = o.optString("isbn", ""),
    title = o.optString("title", ""),
    authors = o.optJSONArray("authors").toStringList(),
    year = o.optInt("year", 0),
    subjects = o.optJSONArray("subjects").toStringList(),
    coverUrl = if (o.isNull("coverUrl")) null else o.optString("coverUrl", "").takeIf { it.isNotBlank() },
    copiesAvailable = o.optInt("copiesAvailable", 0),
)

internal fun JSONArray?.toStringList(): List<String> =
    if (this == null) emptyList() else List(length()) { optString(it, "") }
