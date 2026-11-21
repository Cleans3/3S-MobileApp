package vn.shelftap.app.data.remote

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import vn.shelftap.app.model.Title
import java.io.IOException

/**
 * Network access to the ShelfTap REST API. Every call is HTTPS and runs on [io],
 * so these suspend functions are safe to call from the main thread.
 */
class CatalogueApi(
    private val client: OkHttpClient,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {

    data class Page(
        val items: List<Title>,
        val page: Int,
        val size: Int,
        val total: Int,
    )

    /** GET /api/titles?page=&size= (no text query: search runs on the local copy). */
    suspend fun fetchPage(page: Int, size: Int): Page = withContext(io) {
        val url = BASE_URL.toHttpUrl().newBuilder()
            .addPathSegments("api/titles")
            .addQueryParameter("page", page.toString())
            .addQueryParameter("size", size.toString())
            .build()
        val json = execute(url) ?: throw IOException("Catalogue page $page not found")
        Page(
            items = json.optJSONArray("items").toTitles(),
            page = json.optInt("page", page),
            size = json.optInt("size", size),
            total = json.optInt("total", 0),
        )
    }

    /** GET /api/titles/{id}. Returns null when the server has no such title. */
    suspend fun fetchTitle(id: String): Title? = withContext(io) {
        val url = BASE_URL.toHttpUrl().newBuilder()
            .addPathSegments("api/titles")
            .addPathSegment(id)
            .build()
        execute(url)?.toTitle()
    }

    /** Blocking GET; call only from [io]. Returns null on HTTP 404. */
    private fun execute(url: HttpUrl): JSONObject? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            if (response.code == 404) return null
            if (!response.isSuccessful) throw IOException("Unexpected HTTP ${response.code}")
            val body = response.body?.string() ?: throw IOException("Empty response body")
            return JSONObject(body)
        }
    }

    private fun JSONArray?.toTitles(): List<Title> {
        if (this == null) return emptyList()
        return List(length()) { index -> getJSONObject(index).toTitle() }
    }

    private fun JSONObject.toTitle(): Title = Title(
        id = getString("id"),
        isbn = text("isbn"),
        title = getString("title"),
        authors = optJSONArray("authors").toStrings(),
        year = optInt("year", 0),
        subjects = optJSONArray("subjects").toStrings(),
        // Only HTTPS covers are accepted; anything else is treated as "no cover".
        coverUrl = text("coverUrl").takeIf { it.startsWith("https://") },
        copiesAvailable = optInt("copiesAvailable", 0),
    )

    private fun JSONObject.text(name: String): String =
        if (isNull(name)) "" else optString(name, "")

    private fun JSONArray?.toStrings(): List<String> {
        if (this == null) return emptyList()
        return List(length()) { index -> optString(index, "") }.filter { it.isNotBlank() }
    }

    companion object {
        const val BASE_URL = "https://api.shelftap.example"
    }
}
