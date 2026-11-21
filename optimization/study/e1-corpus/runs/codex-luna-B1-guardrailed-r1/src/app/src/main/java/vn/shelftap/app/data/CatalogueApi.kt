package vn.shelftap.app.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class CatalogueApi {
    suspend fun titles(query: String, page: Int, size: Int): List<Title> = withContext(Dispatchers.IO) {
        val url = Uri.parse("https://api.shelftap.example/api/titles").buildUpon()
            .appendQueryParameter("q", query).appendQueryParameter("page", page.toString()).appendQueryParameter("size", size.toString()).build().toString()
        request(url).let { root ->
            val items = root.getJSONArray("items")
            buildList(items.length()) { for (i in 0 until items.length()) add(parse(items.getJSONObject(i))) }
        }
    }

    suspend fun title(id: String): Title = withContext(Dispatchers.IO) {
        parse(request("https://api.shelftap.example/api/titles/${Uri.encode(id)}"))
    }

    private fun request(urlString: String): JSONObject {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 10_000; readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (connection.responseCode !in 200..299) error("Catalogue request failed")
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(json: JSONObject) = Title(
        json.getString("id"), json.optString("isbn"), json.getString("title"),
        json.optJSONArray("authors").toStrings(), json.optInt("year"),
        json.optJSONArray("subjects").toStrings(), json.optString("coverUrl").takeIf { it.startsWith("https://") },
        json.optInt("copiesAvailable")
    )

    private fun org.json.JSONArray?.toStrings(): List<String> = if (this == null) emptyList() else buildList(length()) { for (i in 0 until length()) add(optString(i)) }
}
