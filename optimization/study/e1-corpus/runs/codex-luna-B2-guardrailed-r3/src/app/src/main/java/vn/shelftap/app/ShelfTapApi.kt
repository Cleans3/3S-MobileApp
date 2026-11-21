package vn.shelftap.app

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ShelfTapApi(private val storage: SessionStore, private val context: android.content.Context) {
    private val client = OkHttpClient.Builder().build()
    private val base = "https://api.shelftap.example".toHttpUrl()
    private val jsonType = "application/json".toMediaType()
    private val refreshLock = Mutex()
    private suspend fun deviceId(): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { DeviceIdStore(context).getOrCreate() }

    private fun request(path: String, method: String, body: JSONObject? = null, token: String? = null): okhttp3.Response {
        val builder = Request.Builder().url(base.newBuilder().addPathSegments(path.removePrefix("/")).build())
            .method(method, body?.toString()?.toRequestBody(jsonType))
        if (token != null) builder.header("Authorization", "Bearer $token")
        return client.newCall(builder.build()).execute()
    }

    private fun parseError(response: okhttp3.Response): Nothing {
        val code = runCatching { JSONObject(response.body?.string().orEmpty()).optString("code").takeIf { it.isNotBlank() } }.getOrNull()
        val retry = response.header("Retry-After")?.toIntOrNull()
        throw ApiException(code, response.code, retry)
    }

    private fun sessionFrom(response: okhttp3.Response): Session {
        response.use { if (!it.isSuccessful) parseError(it); val o = JSONObject(it.body?.string().orEmpty()); return Session(o.getString("accessToken"), o.getString("refreshToken"), o.optBoolean("mustChangePassword")) }
    }

    suspend fun login(email: String, password: String): Session = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        sessionFrom(request("api/auth/login", "POST", JSONObject().put("email", email).put("password", password).put("deviceId", deviceId())))
    }

    private suspend fun refresh(): Session = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        refreshLock.withLock {
            val old = storage.read() ?: throw ApiException("TOKEN_INVALID", 401)
            val next = sessionFrom(request("api/auth/refresh", "POST", JSONObject().put("refreshToken", old.refreshToken).put("deviceId", deviceId())))
            storage.write(next); next
        }
    }

    private suspend fun <T> authenticated(call: (String) -> T): T {
        val current = storage.read() ?: throw ApiException("TOKEN_INVALID", 401)
        return try { call(current.accessToken) } catch (e: ApiException) {
            if (e.status != 401 || e.code != "TOKEN_INVALID") throw e
            call(refresh().accessToken)
        }
    }

    suspend fun member(): Member = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { authenticated { token ->
        request("api/me", "GET", token = token).use { response -> if (!response.isSuccessful) parseError(response); val o = JSONObject(response.body?.string().orEmpty()); Member(o.getString("memberId"), o.getString("fullName"), o.getString("email"), o.getString("memberType"), o.getString("status"), o.optBoolean("historyEnabled"), o.optBoolean("mustChangePassword")) }
    } }

    suspend fun changePassword(current: String, next: String) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { authenticated { token ->
        request("api/me/password", "POST", JSONObject().put("currentPassword", current).put("newPassword", next), token).use { response -> if (!response.isSuccessful) parseError(response) }
    } }

    suspend fun logout() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { storage.read()?.let { session -> runCatching { request("api/auth/logout", "POST", JSONObject().put("refreshToken", session.refreshToken)).use { } } } }
}
