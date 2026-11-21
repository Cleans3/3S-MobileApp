package vn.shelftap.app.data.remote

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

private const val BASE_URL = "https://api.shelftap.example/"

/** Thrown for non-2xx responses. It is an IOException, so the sync treats it as retryable. */
class ApiException(val code: Int) : IOException("HTTP $code")

class CatalogueApi(
    private val client: OkHttpClient,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher,
    private val baseUrl: HttpUrl = BASE_URL.toHttpUrl(),
) {

    /** GET /api/titles. The query is optional; a null or blank query lists everything. */
    suspend fun listTitles(page: Int, size: Int, query: String? = null): TitlePageDto {
        val url = baseUrl.newBuilder()
            .addPathSegment("api")
            .addPathSegment("titles")
            .apply { if (!query.isNullOrBlank()) addQueryParameter("q", query) }
            .addQueryParameter("page", page.toString())
            .addQueryParameter("size", size.toString())
            .build()
        return get(url, TitlePageDto.serializer())
    }

    /** GET /api/titles/{id}. */
    suspend fun getTitle(id: String): TitleDto {
        val url = baseUrl.newBuilder()
            .addPathSegment("api")
            .addPathSegment("titles")
            .addPathSegment(id)
            .build()
        return get(url, TitleDto.serializer())
    }

    private suspend fun <T> get(url: HttpUrl, serializer: KSerializer<T>): T =
        withContext(ioDispatcher) {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw ApiException(response.code)
                val body = response.body?.string() ?: throw IOException("Empty response body")
                json.decodeFromString(serializer, body)
            }
        }
}
