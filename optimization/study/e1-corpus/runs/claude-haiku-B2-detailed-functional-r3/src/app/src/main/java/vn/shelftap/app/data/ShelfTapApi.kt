package vn.shelftap.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/** Thin HTTP client for https://api.shelftap.example. Throws [ApiException] on non-2xx. */
class ShelfTapApi(
    private val client: OkHttpClient,
    private val json: Json,
    private val baseUrl: String,
) {

    suspend fun login(email: String, password: String, deviceId: String): TokenResponse {
        val request = request("/api/auth/login")
            .post(body(LoginRequest(email, password, deviceId)))
            .build()
        return json.decodeFromString(execute(request))
    }

    suspend fun refresh(refreshToken: String, deviceId: String): TokenResponse {
        val request = request("/api/auth/refresh")
            .post(body(RefreshRequest(refreshToken, deviceId)))
            .build()
        return json.decodeFromString(execute(request))
    }

    suspend fun logout(refreshToken: String) {
        val request = request("/api/auth/logout")
            .post(body(LogoutRequest(refreshToken)))
            .build()
        execute(request)
    }

    suspend fun me(accessToken: String): MeResponse {
        val request = request("/api/me")
            .get()
            .bearer(accessToken)
            .build()
        return json.decodeFromString(execute(request))
    }

    suspend fun changePassword(accessToken: String, currentPassword: String, newPassword: String) {
        val request = request("/api/me/password")
            .post(body(ChangePasswordRequest(currentPassword, newPassword)))
            .bearer(accessToken)
            .build()
        execute(request)
    }

    private fun request(path: String): Request.Builder =
        Request.Builder()
            .url(baseUrl + path)
            .header("Accept", "application/json")

    private fun Request.Builder.bearer(accessToken: String): Request.Builder =
        header("Authorization", "Bearer $accessToken")

    private inline fun <reified T> body(value: T): RequestBody =
        json.encodeToString(value).toRequestBody(JSON_MEDIA_TYPE)

    /** Runs the request off the main thread and returns the response body text. */
    private suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                text
            } else {
                throw toApiException(response.code, text, response.header("Retry-After"))
            }
        }
    }

    private fun toApiException(status: Int, body: String, retryAfter: String?): ApiException {
        val problem = runCatching { json.decodeFromString<ProblemResponse>(body) }.getOrNull()
        return ApiException(
            httpStatus = status,
            code = problem?.code,
            detail = problem?.detail ?: problem?.title,
            retryAfterSeconds = retryAfter?.trim()?.toIntOrNull(),
        )
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
