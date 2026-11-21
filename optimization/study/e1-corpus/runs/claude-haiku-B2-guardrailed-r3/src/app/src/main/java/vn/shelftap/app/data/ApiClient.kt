package vn.shelftap.app.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/** Minimal JSON-over-HTTPS client. All calls run on [io]. Response bodies are never logged. */
class ApiClient(
    private val baseUrl: String,
    private val http: OkHttpClient,
    private val io: CoroutineDispatcher,
) {

    suspend fun get(path: String, accessToken: String): JSONObject {
        val request = newRequest(path)
            .header(HEADER_AUTHORIZATION, "Bearer $accessToken")
            .get()
            .build()
        return execute(request)
    }

    /** Returns an empty object for 204 responses. */
    suspend fun post(path: String, body: JSONObject, accessToken: String? = null): JSONObject {
        val builder = newRequest(path).post(body.toString().toRequestBody(JSON_MEDIA))
        if (accessToken != null) {
            builder.header(HEADER_AUTHORIZATION, "Bearer $accessToken")
        }
        return execute(builder.build())
    }

    private fun newRequest(path: String): Request.Builder =
        Request.Builder()
            .url(baseUrl + path)
            .header("Accept", "application/json")

    private suspend fun execute(request: Request): JSONObject = withContext(io) {
        val response = try {
            http.newCall(request).execute()
        } catch (e: IOException) {
            throw NetworkException()
        }
        response.use { r ->
            val text = r.body?.string().orEmpty()
            if (r.isSuccessful) {
                parseSuccess(text, r.code)
            } else {
                throw parseError(r.code, text, r.header("Retry-After"))
            }
        }
    }

    private fun parseSuccess(text: String, status: Int): JSONObject {
        if (text.isBlank()) return JSONObject()
        return try {
            JSONObject(text)
        } catch (e: JSONException) {
            throw ApiException(status, ErrorCodes.INVALID_RESPONSE)
        }
    }

    private fun parseError(status: Int, text: String, retryAfter: String?): ApiException {
        val code = try {
            JSONObject(text).optString("code")
        } catch (e: JSONException) {
            ""
        }
        return ApiException(
            httpStatus = status,
            code = code.ifEmpty { ErrorCodes.UNKNOWN },
            retryAfterSeconds = retryAfter?.trim()?.toIntOrNull(),
        )
    }

    private companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
        val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
