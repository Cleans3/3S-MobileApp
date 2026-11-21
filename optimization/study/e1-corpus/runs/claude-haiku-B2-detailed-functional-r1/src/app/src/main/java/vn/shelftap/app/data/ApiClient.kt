package vn.shelftap.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

private const val BASE_URL = "https://api.shelftap.example"
private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

/**
 * Thin REST client. Each call returns the decoded body, or throws [ApiException]
 * for an HTTP error response, or an IOException when the network fails.
 */
class ApiClient(private val json: Json) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun login(email: String, password: String, deviceId: String): TokenResponse {
        val body = json.encodeToString(LoginRequest(email, password, deviceId))
        return json.decodeFromString(post("/api/auth/login", body, bearer = null))
    }

    suspend fun refresh(refreshToken: String, deviceId: String): TokenResponse {
        val body = json.encodeToString(RefreshRequest(refreshToken, deviceId))
        return json.decodeFromString(post("/api/auth/refresh", body, bearer = null))
    }

    suspend fun logout(refreshToken: String) {
        val body = json.encodeToString(LogoutRequest(refreshToken))
        post("/api/auth/logout", body, bearer = null)
    }

    suspend fun me(accessToken: String): MeResponse {
        return json.decodeFromString(get("/api/me", accessToken))
    }

    suspend fun changePassword(accessToken: String, currentPassword: String, newPassword: String) {
        val body = json.encodeToString(ChangePasswordRequest(currentPassword, newPassword))
        post("/api/me/password", body, bearer = accessToken)
    }

    private suspend fun post(path: String, jsonBody: String, bearer: String?): String {
        val request = Request.Builder()
            .url(BASE_URL + path)
            .post(jsonBody.toRequestBody(JSON_TYPE))
        return execute(request, bearer)
    }

    private suspend fun get(path: String, bearer: String?): String {
        return execute(Request.Builder().url(BASE_URL + path).get(), bearer)
    }

    private suspend fun execute(builder: Request.Builder, bearer: String?): String =
        withContext(Dispatchers.IO) {
            builder.header("Accept", "application/json")
            if (bearer != null) {
                builder.header("Authorization", "Bearer $bearer")
            }
            http.newCall(builder.build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw parseError(response.code, response.header("Retry-After"), text)
                }
                text
            }
        }

    private fun parseError(status: Int, retryAfter: String?, text: String): ApiException {
        val problem = runCatching { json.decodeFromString<ProblemDetails>(text) }.getOrNull()
        return ApiException(
            httpStatus = status,
            code = problem?.code,
            detail = problem?.detail ?: problem?.title,
            retryAfterSeconds = retryAfter?.trim()?.toIntOrNull(),
        )
    }
}
