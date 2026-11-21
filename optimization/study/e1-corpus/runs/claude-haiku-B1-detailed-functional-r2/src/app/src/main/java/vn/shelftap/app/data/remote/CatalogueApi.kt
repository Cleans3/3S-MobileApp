package vn.shelftap.app.data.remote

import org.json.JSONArray
import org.json.JSONObject
import vn.shelftap.app.model.Title
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Minimal client for the ShelfTap REST API. Blocking calls: run them off the main thread. */
class CatalogueApi(private val baseUrl: String = BASE_URL) {

    data class TitlePage(
        val items: List<Title>,
        val page: Int,
        val size: Int,
        val total: Int,
    )

    /** GET /api/titles?q=&page=&size=. The query is optional. */
    fun fetchPage(query: String?, page: Int, size: Int): TitlePage {
        val params = buildString {
            if (!query.isNullOrBlank()) {
                append("q=").append(encode(query.trim())).append('&')
            }
            append("page=").append(page)
            append("&size=").append(size.coerceIn(1, MAX_PAGE_SIZE))
        }
        val root = JSONObject(get("$baseUrl/api/titles?$params"))
        val items = root.getJSONArray("items")
        return TitlePage(
            items = List(items.length()) { parseTitle(items.getJSONObject(it)) },
            page = root.optInt("page", page),
            size = root.optInt("size", size),
            total = root.optInt("total", 0),
        )
    }

    /** GET /api/titles/{id}. */
    fun fetchTitle(id: String): Title =
        parseTitle(JSONObject(get("$baseUrl/api/titles/${encode(id).replace("+", "%20")}")))

    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun parseTitle(o: JSONObject): Title = Title(
        id = o.getString("id"),
        isbn = o.optString("isbn"),
        title = o.optString("title"),
        authors = o.optJSONArray("authors").toStringList(),
        year = o.optInt("year"),
        subjects = o.optJSONArray("subjects").toStringList(),
        coverUrl = if (o.isNull("coverUrl")) null else o.optString("coverUrl").ifBlank { null },
        copiesAvailable = o.optInt("copiesAvailable"),
    )

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun JSONArray?.toStringList(): List<String> =
        if (this == null) emptyList() else List(length()) { optString(it) }

    companion object {
        const val BASE_URL = "https://api.shelftap.example"
        const val MAX_PAGE_SIZE = 50
    }
}
