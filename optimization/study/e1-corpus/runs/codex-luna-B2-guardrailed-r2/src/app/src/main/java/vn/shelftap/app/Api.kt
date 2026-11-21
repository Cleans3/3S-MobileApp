package vn.shelftap.app

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class Tokens(val accessToken: String, val refreshToken: String)
data class Member(
    val memberId: String,
    val fullName: String,
    val email: String,
    val memberType: String,
    val status: String,
    val historyEnabled: Boolean,
    val mustChangePassword: Boolean
)
data class ApiFailure(val status: Int, val code: String?, val detail: String?, val retryAfterSeconds: Long?) : Exception()

class ShelfTapApi(private val io: CoroutineDispatcher = Dispatchers.IO) {
    private val client = OkHttpClient()
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val baseUrl = "https://api.shelftap.example"

    suspend fun login(email: String, password: String, deviceId: String): Pair<Tokens, Boolean> =
        post("/api/auth/login", JSONObject().apply {
            put("email", email); put("password", password); put("deviceId", deviceId)
        }).let { parseTokenResponse(it) }

    suspend fun refresh(refreshToken: String, deviceId: String): Pair<Tokens, Boolean> =
        post("/api/auth/refresh", JSONObject().apply {
            put("refreshToken", refreshToken); put("deviceId", deviceId)
        }).let { parseTokenResponse(it) }

    suspend fun logout(refreshToken: String) {
        request("POST", "/api/auth/logout", JSONObject().put("refreshToken", refreshToken), null)
    }

    suspend fun me(accessToken: String): Member = parseMember(request("GET", "/api/me", null, accessToken))

    suspend fun changePassword(accessToken: String, current: String, new: String) {
        request("POST", "/api/me/password", JSONObject().apply {
            put("currentPassword", current); put("newPassword", new)
        }, accessToken)
    }

    private suspend fun post(path: String, body: JSONObject): String = request("POST", path, body, null)

    private suspend fun request(method: String, path: String, body: JSONObject?, token: String?): String = withContext(io) {
        val builder = Request.Builder().url(baseUrl + path)
        if (token != null) builder.header("Authorization", "Bearer $token")
        if (body != null) builder.method(method, body.toString().toRequestBody(jsonType)) else builder.method(method, null)
        client.newCall(builder.build()).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val error = runCatching { JSONObject(responseBody) }.getOrNull()
                throw ApiFailure(
                    response.code,
                    error?.optString("code")?.takeIf { it.isNotBlank() },
                    error?.optString("detail")?.takeIf { it.isNotBlank() },
                    response.header("Retry-After")?.toLongOrNull()
                )
            }
            responseBody
        }
    }

    private fun parseTokenResponse(body: String): Pair<Tokens, Boolean> {
        val json = JSONObject(body)
        return Tokens(json.getString("accessToken"), json.getString("refreshToken")) to
            json.optBoolean("mustChangePassword", false)
    }

    private fun parseMember(body: String): Member {
        val json = JSONObject(body)
        return Member(
            json.getString("memberId"), json.getString("fullName"), json.getString("email"),
            json.getString("memberType"), json.getString("status"),
            json.optBoolean("historyEnabled", false), json.optBoolean("mustChangePassword", false)
        )
    }
}
