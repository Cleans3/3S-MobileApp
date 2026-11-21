package vn.shelftap.app.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

@Serializable
data class TitleDto(
    val id: String,
    val isbn: String = "",
    val title: String,
    val authors: List<String> = emptyList(),
    val year: Int = 0,
    val subjects: List<String> = emptyList(),
    val coverUrl: String? = null,
    val copiesAvailable: Int = 0
)

@Serializable
data class TitlePageDto(
    val items: List<TitleDto> = emptyList(),
    val page: Int = 0,
    val size: Int = 0,
    val total: Int = 0
)

/**
 * Minimal REST client for https://api.shelftap.example.
 * Every call runs on [ioDispatcher], so it is safe to call from the main thread.
 * Responses are never logged: they contain member-related data.
 */
class TitleApi(
    private val client: OkHttpClient,
    private val json: Json,
    private val baseUrl: HttpUrl,
    private val ioDispatcher: CoroutineDispatcher
) {
    constructor(
        client: OkHttpClient,
        json: Json,
        baseUrl: String,
        ioDispatcher: CoroutineDispatcher
    ) : this(client, json, baseUrl.toHttpUrl(), ioDispatcher)

    suspend fun fetchPage(page: Int, size: Int): TitlePageDto = withContext(ioDispatcher) {
        val url = baseUrl.newBuilder()
            .addPathSegment("api")
            .addPathSegment("titles")
            .addQueryParameter("page", page.toString())
            .addQueryParameter("size", size.toString())
            .build()
        get(url) { body -> json.decodeFromString(TitlePageDto.serializer(), body) }
    }

    suspend fun fetchTitle(id: String): TitleDto = withContext(ioDispatcher) {
        val url = baseUrl.newBuilder()
            .addPathSegment("api")
            .addPathSegment("titles")
            .addPathSegment(id)
            .build()
        get(url) { body -> json.decodeFromString(TitleDto.serializer(), body) }
    }

    private fun <T> get(url: HttpUrl, parse: (String) -> T): T {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body?.string() ?: throw IOException("Empty response body")
            return parse(body)
        }
    }
}
