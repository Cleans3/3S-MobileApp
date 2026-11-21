package vn.shelftap.app

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ApiClient {
    private val base = "https://api.shelftap.example"
    fun page(query: String, page: Int, size: Int): Page {
        val q = URLEncoder.encode(query, "UTF-8"); val json = get("$base/api/titles?q=$q&page=$page&size=$size")
        val items = json.optJSONArray("items")?.let(::parseTitles) ?: emptyList()
        return Page(items, json.optInt("page", page), json.optInt("size", size), json.optInt("total", items.size))
    }
    fun title(id: String): Title { return parseTitle(JSONObject(get("$base/api/titles/${URLEncoder.encode(id, "UTF-8")}"))) }
    private fun get(url: String): String { val c = (URL(url).openConnection() as HttpURLConnection).apply { requestMethod = "GET"; connectTimeout = 8000; readTimeout = 12000 }; return c.inputStream.bufferedReader().use { it.readText() }.also { c.disconnect() } }
    private fun parseTitles(a: JSONArray) = (0 until a.length()).map { parseTitle(a.getJSONObject(it)) }
    private fun parseTitle(o: JSONObject) = Title(o.optString("id"), o.optString("isbn"), o.optString("title"), strings(o.optJSONArray("authors")), o.optInt("year"), strings(o.optJSONArray("subjects")), o.optString("coverUrl").takeIf { it.isNotBlank() }, o.optInt("copiesAvailable"))
    private fun strings(a: JSONArray?) = if (a == null) emptyList() else (0 until a.length()).map { a.optString(it) }
}
data class Page(val items: List<Title>, val page: Int, val size: Int, val total: Int)
