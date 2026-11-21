package vn.shelftap.app.data.api

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/**
 * Stateless HTTPS client for the ShelfTap REST API. Tokens are passed in by the caller and
 * are never logged.
 */
internal class ShelfTapApi(
    private val client: OkHttpClient,
    private val json: Json,
    private val baseUrl: String,
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun login(email: String, password: String, deviceId: String): TokenResponse =
        execute(post("/api/auth/login", json.encodeToString(LoginRequest(email, password, deviceId)))) {
            json.decodeFromString<TokenResponse>(it)
        }

    suspend fun refresh(refreshToken: String, deviceId: String): TokenResponse =
        execute(post("/api/auth/refresh", json.encodeToString(RefreshRequest(refreshToken, deviceId)))) {
            json.decodeFromString<TokenResponse>(it)
        }

    suspend fun logout(refreshToken: String) {
        execute(post("/api/auth/logout", json.encodeToString(LogoutRequest(refreshToken)))) { Unit }
    }

    suspend fun me(accessToken: String): MeResponse =
        execute(get("/api/me", accessToken)) { json.decodeFromString<MeResponse>(it) }

    suspend fun changePassword(accessToken: String, currentPassword: String, newPassword: String) {
        val payload = json.encodeToString(ChangePasswordRequest(currentPassword, newPassword))
        execute(post("/api/me/password", payload, accessToken)) { Unit }
    }

    private fun post(path: String, payload: String, accessToken: String? = null): Request {
        val builder = Request.Builder()
            .url(baseUrl + path)
            .header("Accept", "application/json")
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
        if (accessToken != null) {
            builder.header("Authorization", "Bearer $accessToken")
        }
        return builder.build()
    }

    private fun get(path: String, accessToken: String): Request =
        Request.Builder()
            .url(baseUrl + path)
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

    private suspend fun <T> execute(request: Request, decode: (String) -> T): T =
        withContext(ioDispatcher) {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw toApiException(response, body)
                }
                decode(body)
            }
        }

    private fun toApiException(response: Response, body: String): ApiException {
        val code = runCatching { json.decodeFromString<ProblemResponse>(body).code }.getOrNull()
        val retryAfter = response.header("Retry-After")?.trim()?.toIntOrNull()
        return ApiException(httpStatus = response.code, code = code, retryAfterSeconds = retryAfter)
    }

    private companion object {
        val JSON_MEDIA_TYPE: MediaType = "application/json; charset=utf-8".toMediaType()
    }
}
