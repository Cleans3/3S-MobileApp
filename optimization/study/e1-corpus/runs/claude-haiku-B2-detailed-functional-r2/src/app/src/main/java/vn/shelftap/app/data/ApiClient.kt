package vn.shelftap.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/** An error response from the API. [code] is the server's error code, e.g. TOKEN_INVALID. */
class ApiException(
    val httpStatus: Int,
    val code: String?,
    override val message: String,
    val retryAfterSeconds: Int? = null,
) : Exception(message) {
    val isTokenInvalid: Boolean
        get() = httpStatus == 401 && code == "TOKEN_INVALID"
}

/** Thrown when the session can no longer be renewed. The stored session has been removed. */
class SessionExpiredException : Exception("Your session has expired. Please sign in again.")

/** Thin HTTP layer. Returns the response body as text, or throws [ApiException] / IOException. */
class ApiClient(
    private val baseUrl: String,
    private val http: OkHttpClient,
    private val json: Json,
) {
    suspend fun send(
        method: String,
        path: String,
        bearer: String? = null,
        jsonBody: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + path)
            .header("Accept", "application/json")
            .apply { if (bearer != null) header("Authorization", "Bearer $bearer") }
            .method(method, bodyFor(method, jsonBody))
            .build()

        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                text
            } else {
                throw parseError(response.code, response.header("Retry-After"), text)
            }
        }
    }

    private fun bodyFor(method: String, jsonBody: String?): RequestBody? = when {
        jsonBody != null -> jsonBody.toRequestBody(JSON_TYPE)
        method == "GET" -> null
        else -> ByteArray(0).toRequestBody(null) // POST/PUT must have a body in OkHttp
    }

    private fun parseError(status: Int, retryAfter: String?, text: String): ApiException {
        val problem = runCatching { json.decodeFromString<ProblemDto>(text) }.getOrNull()
        return ApiException(
            httpStatus = status,
            code = problem?.code,
            message = problem?.detail ?: problem?.title ?: "Request failed ($status).",
            retryAfterSeconds = retryAfter?.trim()?.toIntOrNull(),
        )
    }

    private companion object {
        val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
