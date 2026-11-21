package vn.shelftap.app.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShelfTapApi(private val auth: AuthRepository, private val io: CoroutineDispatcher = Dispatchers.IO) {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val base = "https://api.shelftap.example".toHttpUrl()
    private suspend inline fun <reified T> get(path: String): T = request(path, "GET").let { json.decodeFromString(it) }
    private suspend fun request(path: String, method: String, body: String? = null): String = withContext(io) {
        val url = base.newBuilder().encodedPath(path).build()
        val builder = Request.Builder().url(url).header("Authorization", "Bearer ${auth.accessToken()}")
        if (method == "POST") builder.post((body ?: "{}").toRequestBody(null)) else if (method == "PUT") builder.put((body ?: "{}").toRequestBody(null)) else builder.get()
        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) throw ApiException(response.code, response.body?.string().orEmpty())
            response.body?.string() ?: ""
        }
    }
    suspend fun loans() = get<List<Loan>>("/api/me/loans")
    suspend fun history() = get<List<HistoryItem>>("/api/me/history")
    suspend fun member() = get<Member>("/api/me")
    suspend fun renew(id: String) = request("/api/loans/$id/renew", "POST").let { json.decodeFromString<Loan>(it) }
    suspend fun setHistory(enabled: Boolean): Member = request("/api/me/privacy", "PUT", json.encodeToString(mapOf("historyEnabled" to enabled))).let { json.decodeFromString(it) }
    suspend fun title(id: String) = get<TitleDetail>("/api/titles/$id")
}
class ApiException(val status: Int, val payload: String) : Exception()
