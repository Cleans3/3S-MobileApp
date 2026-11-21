package vn.shelftap.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

const val API_BASE_URL = "https://api.shelftap.example"

private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

/** Thin HTTP layer: one function per endpoint, no token logic. */
class ShelfTapApi(
    private val client: OkHttpClient,
    private val json: Json,
) {

    suspend fun login(email: String, password: String, deviceId: String): TokenResponse =
        send(
            method = "POST",
            path = "/api/auth/login",
            bearer = null,
            body = json.encodeToString(
                LoginRequest.serializer(),
                LoginRequest(email, password, deviceId),
            ),
        ) { json.decodeFromString(TokenResponse.serializer(), it) }

    suspend fun refresh(refreshToken: String, deviceId: String): TokenResponse =
        send(
            method = "POST",
            path = "/api/auth/refresh",
            bearer = null,
            body = json.encodeToString(
                RefreshRequest.serializer(),
                RefreshRequest(refreshToken, deviceId),
            ),
        ) { json.decodeFromString(TokenResponse.serializer(), it) }

    suspend fun logout(refreshToken: String) {
        send(
            method = "POST",
            path = "/api/auth/logout",
            bearer = null,
            body = json.encodeToString(LogoutRequest.serializer(), LogoutRequest(refreshToken)),
        ) { }
    }

    suspend fun me(accessToken: String): Me =
        send(
            method = "GET",
            path = "/api/me",
            bearer = accessToken,
            body = null,
        ) { json.decodeFromString(Me.serializer(), it) }

    suspend fun changePassword(accessToken: String, currentPassword: String, newPassword: String) {
        send(
            method = "POST",
            path = "/api/me/password",
            bearer = accessToken,
            body = json.encodeToString(
                ChangePasswordRequest.serializer(),
                ChangePasswordRequest(currentPassword, newPassword),
            ),
        ) { }
    }

    private suspend fun <T> send(
        method: String,
        path: String,
        bearer: String?,
        body: String?,
        parse: (String) -> T,
    ): T = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(API_BASE_URL + path)
            .header("Accept", "application/json")
            .apply { if (bearer != null) header("Authorization", "Bearer $bearer") }
            .method(method, body?.toRequestBody(JSON_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                parse(text)
            } else {
                throw apiError(response.code, response.header("Retry-After"), text)
            }
        }
    }

    private fun apiError(status: Int, retryAfter: String?, text: String): ApiException {
        val problem = runCatching { json.decodeFromString(ProblemDetails.serializer(), text) }
            .getOrNull()
        return ApiException(
            status = status,
            code = problem?.code,
            detail = problem?.detail ?: problem?.title,
            retryAfterSeconds = retryAfter?.trim()?.toLongOrNull(),
        )
    }
}
