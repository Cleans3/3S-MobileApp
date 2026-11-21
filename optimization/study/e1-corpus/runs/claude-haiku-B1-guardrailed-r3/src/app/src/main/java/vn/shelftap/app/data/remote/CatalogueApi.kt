package vn.shelftap.app.data.remote

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import vn.shelftap.app.model.Title
import java.io.IOException

class TitlePage(
    val items: List<Title>,
    val page: Int,
    val size: Int,
    val total: Int,
)

/** Calls the ShelfTap REST API over HTTPS. Uses only the Android SDK's org.json for parsing. */
class CatalogueApi(
    private val client: OkHttpClient,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val baseUrl: HttpUrl = BASE_URL.toHttpUrl()

    /** GET /api/titles?page=&size= (q is omitted, so the whole catalogue is listed). */
    suspend fun listPage(page: Int, size: Int): TitlePage = withContext(ioDispatcher) {
        val url = baseUrl.newBuilder()
            .addPathSegment("api")
            .addPathSegment("titles")
            .addQueryParameter("page", page.toString())
            .addQueryParameter("size", size.toString())
            .build()
        val root = JSONObject(get(url) ?: throw IOException("Catalogue page not found"))
        val items = root.getJSONArray("items")
        TitlePage(
            items = List(items.length()) { items.getJSONObject(it).toTitle() },
            page = root.optInt("page", page),
            size = root.optInt("size", size),
            total = root.getInt("total"),
        )
    }

    /** GET /api/titles/{id}. Returns null when the server answers 404. */
    suspend fun findById(id: String): Title? = withContext(ioDispatcher) {
        val url = baseUrl.newBuilder()
            .addPathSegment("api")
            .addPathSegment("titles")
            .addPathSegment(id)
            .build()
        val body = get(url) ?: return@withContext null
        JSONObject(body).toTitle()
    }

    private fun get(url: HttpUrl): String? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (response.code == 404) return null
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            return response.body?.string() ?: throw IOException("Empty response")
        }
    }

    private companion object {
        const val BASE_URL = "https://api.shelftap.example/"
    }
}

private fun JSONObject.toTitle(): Title = Title(
    id = getString("id"),
    isbn = optString("isbn"),
    title = getString("title"),
    authors = optJSONArray("authors").toStrings(),
    year = optInt("year"),
    subjects = optJSONArray("subjects").toStrings(),
    // Only https covers are accepted; anything else is treated as absent.
    coverUrl = if (isNull("coverUrl")) null else optString("coverUrl").takeIf { it.startsWith("https://") },
    copiesAvailable = optInt("copiesAvailable"),
)

private fun JSONArray?.toStrings(): List<String> =
    if (this == null) emptyList() else List(length()) { getString(it) }
