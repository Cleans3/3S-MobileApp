package vn.shelftap.app.data.remote

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
data class TitleDto(
    val id: String,
    val isbn: String = "",
    val title: String = "",
    val authors: List<String> = emptyList(),
    val year: Int = 0,
    val subjects: List<String> = emptyList(),
    val coverUrl: String? = null,
    val copiesAvailable: Int = 0,
)

@Serializable
data class TitlePageDto(
    val items: List<TitleDto> = emptyList(),
    val page: Int = 0,
    val size: Int = 0,
    val total: Int = 0,
)

/** Thin client for https://api.shelftap.example. */
class CatalogueApi(private val client: OkHttpClient) {

    private val json = Json { ignoreUnknownKeys = true }

    /** GET /api/titles?page=&size= (page starts at 0). */
    suspend fun titles(page: Int, size: Int): TitlePageDto {
        val url = baseUrl()
            .addPathSegments("api/titles")
            .addQueryParameter("page", page.toString())
            .addQueryParameter("size", size.toString())
            .build()
        return get(url, TitlePageDto.serializer()) ?: throw IOException("Catalogue page not found")
    }

    /** GET /api/titles/{id}. Returns null when the server does not know the id. */
    suspend fun title(id: String): TitleDto? {
        val url = baseUrl()
            .addPathSegments("api/titles")
            .addPathSegment(id)
            .build()
        return get(url, TitleDto.serializer())
    }

    private fun baseUrl(): HttpUrl.Builder =
        HttpUrl.Builder().scheme("https").host(HOST)

    private suspend fun <T> get(url: HttpUrl, strategy: DeserializationStrategy<T>): T? =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.code == 404) return@withContext null
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body?.string() ?: throw IOException("Empty response")
                json.decodeFromString(strategy, body)
            }
        }

    companion object {
        const val HOST = "api.shelftap.example"
    }
}
