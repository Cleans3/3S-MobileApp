package vn.shelftap.app

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

class TitleApi {
    private val base = "https://api.shelftap.example"

    fun fetchPage(query: String, page: Int, size: Int): List<Title> {
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        return get("$base/api/titles?q=$encoded&page=$page&size=$size").let { root ->
            val array = root.optJSONArray("items") ?: JSONArray()
            List(array.length()) { parse(array.getJSONObject(it)) }
        }
    }

    fun fetch(id: String): Title = parse(get("$base/api/titles/${URLEncoder.encode(id, Charsets.UTF_8.name())}"))

    private fun get(address: String): JSONObject {
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 10_000; readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
        }
        return try { if (connection.responseCode !in 200..299) error("Catalogue request failed") else JSONObject(connection.inputStream.bufferedReader().use { reader -> reader.readText() }) } finally { connection.disconnect() }
    }

    private fun parse(json: JSONObject) = Title(
        json.getString("id"), json.optString("isbn"), json.getString("title"), strings(json.optJSONArray("authors")),
        json.optInt("year"), strings(json.optJSONArray("subjects")), json.optString("coverUrl").takeIf { it.startsWith("https://") }, json.optInt("copiesAvailable")
    )
    private fun strings(array: JSONArray?): List<String> = array?.let { List(it.length()) { index -> it.getString(index) } } ?: emptyList()
}
