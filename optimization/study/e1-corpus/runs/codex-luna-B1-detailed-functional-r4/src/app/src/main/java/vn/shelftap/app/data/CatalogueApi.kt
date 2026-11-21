package vn.shelftap.app.data

import org.json.JSONObject
import vn.shelftap.app.model.Title
import java.net.HttpURLConnection
import java.net.URL

class CatalogueApi {
    private val base = "https://api.shelftap.example"

    fun page(query: String = "", page: Int, size: Int = 50): Page {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val connection = (URL("$base/api/titles?q=$encoded&page=$page&size=$size").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 10_000; readTimeout = 20_000
        }
        return try {
            val c = connection
            if (c.responseCode !in 200..299) error("Catalogue request failed: ${c.responseCode}")
            val root = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
            val items = root.getJSONArray("items").let { array -> (0 until array.length()).map { parse(array.getJSONObject(it)) } }
            Page(items, root.optInt("page", page), root.optInt("size", size), root.optInt("total", items.size))
        } finally { connection.disconnect() }
    }

    private fun parse(o: JSONObject) = Title(o.getString("id"), o.optString("isbn"), o.getString("title"), o.array("authors"), o.optInt("year"), o.array("subjects"), o.optString("coverUrl").takeIf { it.isNotBlank() }, o.optInt("copiesAvailable"))
    private fun JSONObject.array(key: String): List<String> = optJSONArray(key)?.let { (0 until it.length()).map(it::getString) } ?: emptyList()
    data class Page(val items: List<Title>, val page: Int, val size: Int, val total: Int)
}
