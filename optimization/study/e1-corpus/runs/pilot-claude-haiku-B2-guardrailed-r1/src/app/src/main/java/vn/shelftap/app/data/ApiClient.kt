package vn.shelftap.app.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** An HTTP error from the ShelfTap API. [code] is the error's "code" field, for example TOKEN_INVALID. */
class ApiException(
    val status: Int,
    val code: String?,
    val retryAfterSeconds: Int?,
) : Exception("ShelfTap API error: HTTP $status")

/**
 * Blocking HTTP client for the ShelfTap REST API. Call [send] only from a background dispatcher.
 * Only HTTPS is used; redirects are not followed, so a server cannot downgrade the connection.
 */
class ApiClient(private val http: OkHttpClient = defaultHttpClient()) {

    /** Sends one request and returns the response body. Throws [ApiException] for non-2xx responses. */
    fun send(method: String, path: String, bearer: String?, body: JSONObject?): String {
        val builder = Request.Builder()
            .url(BASE_URL + path)
            .header("Accept", "application/json")
        if (bearer != null) {
            builder.header("Authorization", "Bearer $bearer")
        }
        builder.method(method, body?.toString()?.toRequestBody(JSON_TYPE))

        http.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (response.code !in 200..299) {
                throw ApiException(
                    status = response.code,
                    code = parseCode(text),
                    retryAfterSeconds = response.header("Retry-After")?.trim()?.toIntOrNull(),
                )
            }
            return text
        }
    }

    private fun parseCode(text: String): String? = try {
        JSONObject(text).optString("code").ifEmpty { null }
    } catch (e: JSONException) {
        null
    }

    companion object {
        const val BASE_URL = "https://api.shelftap.example"
        private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }
}
