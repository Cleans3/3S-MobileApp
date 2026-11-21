package vn.shelftap.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ApiException(
    val code: String?,
    val status: Int,
    val retryAfterSeconds: Int? = null
) : Exception()

class ApiClient {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json".toMediaType()
    private val baseUrl = "https://api.shelftap.example"

    suspend fun call(method: String, path: String, body: JSONObject? = null, accessToken: String? = null): JSONObject? =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder().url(baseUrl + path).header("Accept", "application/json")
            if (accessToken != null) builder.header("Authorization", "Bearer $accessToken")
            if (body != null) builder.method(method, body.toString().toRequestBody(jsonMediaType))
            else builder.method(method, null)

            client.newCall(builder.build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val error = runCatching { JSONObject(text) }.getOrNull()
                    throw ApiException(
                        code = error?.optString("code")?.takeIf { it.isNotEmpty() },
                        status = response.code,
                        retryAfterSeconds = response.header("Retry-After")?.toIntOrNull()
                    )
                }
                if (text.isBlank()) null else JSONObject(text)
            }
        }
}
