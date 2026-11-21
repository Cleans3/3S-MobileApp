package vn.shelftap.app.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Thin HTTPS client for the ShelfTap REST API. Every call runs on the injected dispatcher,
 * so it is safe to call from the main thread.
 */
class ShelfTapApi(
    private val client: OkHttpClient,
    private val dispatcher: CoroutineDispatcher,
) {
    suspend fun login(email: String, password: String, deviceId: String): TokenPair {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .put("deviceId", deviceId)
        return execute(post("api/auth/login", body)).ensureSuccess().toTokenPair()
    }

    suspend fun refresh(refreshToken: String, deviceId: String): TokenPair {
        val body = JSONObject()
            .put("refreshToken", refreshToken)
            .put("deviceId", deviceId)
        return execute(post("api/auth/refresh", body)).ensureSuccess().toTokenPair()
    }

    suspend fun logout(refreshToken: String) {
        val body = JSONObject().put("refreshToken", refreshToken)
        execute(post("api/auth/logout", body)).ensureSuccess()
    }

    suspend fun me(accessToken: String): Member {
        val request = Request.Builder()
            .url(BASE_URL + "api/me")
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        return execute(request).ensureSuccess().toMember()
    }

    suspend fun changePassword(accessToken: String, currentPassword: String, newPassword: String) {
        val body = JSONObject()
            .put("currentPassword", currentPassword)
            .put("newPassword", newPassword)
        execute(post("api/me/password", body, accessToken)).ensureSuccess()
    }

    private fun post(path: String, body: JSONObject, accessToken: String? = null): Request {
        val builder = Request.Builder()
            .url(BASE_URL + path)
            .header("Accept", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
        if (accessToken != null) {
            builder.header("Authorization", "Bearer $accessToken")
        }
        return builder.build()
    }

    private suspend fun execute(request: Request): ApiResponse = withContext(dispatcher) {
        client.newCall(request).execute().use { response ->
            ApiResponse(
                status = response.code,
                body = response.body?.string().orEmpty(),
                retryAfterSeconds = response.header("Retry-After")?.trim()?.toIntOrNull(),
            )
        }
    }

    private companion object {
        const val BASE_URL = "https://api.shelftap.example/"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

private class ApiResponse(
    val status: Int,
    val body: String,
    val retryAfterSeconds: Int?,
) {
    fun ensureSuccess(): ApiResponse {
        if (status in 200..299) return this
        val code = runCatching { JSONObject(body).optString("code") }
            .getOrNull()
            .orEmpty()
            .ifEmpty { "UNKNOWN" }
        throw ApiException(code = code, httpStatus = status, retryAfterSeconds = retryAfterSeconds)
    }

    fun toTokenPair(): TokenPair {
        val json = JSONObject(body)
        return TokenPair(
            accessToken = json.getString("accessToken"),
            refreshToken = json.getString("refreshToken"),
            mustChangePassword = json.getBoolean("mustChangePassword"),
        )
    }

    fun toMember(): Member {
        val json = JSONObject(body)
        return Member(
            memberId = json.optString("memberId"),
            fullName = json.optString("fullName"),
            email = json.optString("email"),
            memberType = json.optString("memberType"),
            status = json.optString("status"),
            historyEnabled = json.optBoolean("historyEnabled"),
            mustChangePassword = json.optBoolean("mustChangePassword"),
        )
    }
}
