package vn.shelftap.app.data

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class CatalogueApi(private val client: OkHttpClient) {
    private val base = "https://api.shelftap.example".toHttpUrl()

    suspend fun page(query: String, page: Int, size: Int): Page {
        val url = base.newBuilder().addPathSegments("api/titles").addQueryParameter("q", query).addQueryParameter("page", page.toString()).addQueryParameter("size", size.toString()).build()
        val response = client.newCall(Request.Builder().url(url).get().build()).execute()
        response.use { checked ->
            if (!checked.isSuccessful) error("Catalogue request failed")
            return parsePage(checked.body?.string().orEmpty())
        }
    }

    suspend fun title(id: String): Title {
        val url = base.newBuilder().addPathSegments("api/titles/$id").build()
        val response = client.newCall(Request.Builder().url(url).get().build()).execute()
        response.use { checked ->
            if (!checked.isSuccessful) error("Title request failed")
            return parseTitle(JSONObject(checked.body?.string().orEmpty()))
        }
    }

    private fun parsePage(raw: String): Page {
        val json = JSONObject(raw); val array = json.optJSONArray("items") ?: JSONArray()
        return Page((0 until array.length()).map { parseTitle(array.getJSONObject(it)) }, json.optInt("total"))
    }
    private fun parseTitle(json: JSONObject) = Title(
        json.optString("id"), json.optString("isbn"), json.optString("title"), strings(json.optJSONArray("authors")),
        json.optInt("year"), strings(json.optJSONArray("subjects")), json.optString("coverUrl").takeIf { it.isNotBlank() }, json.optInt("copiesAvailable")
    )
    private fun strings(array: JSONArray?) = if (array == null) emptyList() else (0 until array.length()).map { array.optString(it) }
    data class Page(val items: List<Title>, val total: Int)
}
