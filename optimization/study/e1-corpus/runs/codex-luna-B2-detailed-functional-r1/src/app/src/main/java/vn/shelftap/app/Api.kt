package vn.shelftap.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val BASE_URL = "https://api.shelftap.example"

data class Tokens(val accessToken: String, val refreshToken: String, val accessExpiresIn: Long, val mustChangePassword: Boolean)
data class Member(val memberId: String, val fullName: String, val email: String, val memberType: String, val status: String, val historyEnabled: Boolean, val mustChangePassword: Boolean)
class ApiException(val code: String?, val httpStatus: Int, val retryAfter: Long? = null, message: String) : Exception(message)

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("shelftap_session", Context.MODE_PRIVATE)
    val deviceId: String get() = prefs.getString("deviceId", null) ?: java.util.UUID.randomUUID().toString().also { prefs.edit().putString("deviceId", it).apply() }
    fun tokens(): Tokens? = prefs.getString("accessToken", null)?.let { Tokens(it, prefs.getString("refreshToken", "") ?: "", prefs.getLong("expiresIn", 0), prefs.getBoolean("mustChange", false)) }
    fun save(t: Tokens) { prefs.edit().putString("accessToken", t.accessToken).putString("refreshToken", t.refreshToken).putLong("expiresIn", t.accessExpiresIn).putBoolean("mustChange", t.mustChangePassword).apply() }
    fun setMustChange(value: Boolean) { prefs.edit().putBoolean("mustChange", value).apply() }
    fun clear() { prefs.edit().remove("accessToken").remove("refreshToken").remove("expiresIn").remove("mustChange").apply() }
}

class ApiClient(private val store: SessionStore) {
    private suspend fun request(method: String, path: String, body: JSONObject? = null, auth: Boolean = false): JSONObject? = withContext(Dispatchers.IO) {
        val c = (URL(BASE_URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method; connectTimeout = 15000; readTimeout = 15000; setRequestProperty("Accept", "application/json")
            if (auth) setRequestProperty("Authorization", "Bearer ${store.tokens()?.accessToken}")
            if (body != null) { doOutput = true; setRequestProperty("Content-Type", "application/json"); outputStream.use { it.write(body.toString().toByteArray()) } }
        }
        val status = c.responseCode
        val stream = if (status in 200..299) c.inputStream else c.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (status !in 200..299) {
            val json = runCatching { JSONObject(text) }.getOrDefault(JSONObject())
            throw ApiException(json.optString("code", null), status, c.getHeaderField("Retry-After")?.toLongOrNull(), json.optString("detail", "Request failed"))
        }
        if (text.isBlank()) null else JSONObject(text)
    }
    private fun tokens(j: JSONObject) = Tokens(j.getString("accessToken"), j.getString("refreshToken"), j.optLong("accessExpiresIn"), j.optBoolean("mustChangePassword"))
    suspend fun login(email: String, password: String): Tokens = tokens(request("POST", "/api/auth/login", JSONObject().put("email", email).put("password", password).put("deviceId", store.deviceId))!!)
    suspend fun refresh(): Tokens = tokens(request("POST", "/api/auth/refresh", JSONObject().put("refreshToken", store.tokens()?.refreshToken).put("deviceId", store.deviceId))!!)
    suspend fun logout() { runCatching { request("POST", "/api/auth/logout", JSONObject().put("refreshToken", store.tokens()?.refreshToken)) } }
    suspend fun password(current: String, new: String) { request("POST", "/api/me/password", JSONObject().put("currentPassword", current).put("newPassword", new), true) }
    suspend fun me(): Member {
        val j = request("GET", "/api/me", auth = true)!!
        return Member(j.getString("memberId"), j.getString("fullName"), j.getString("email"), j.getString("memberType"), j.getString("status"), j.optBoolean("historyEnabled"), j.optBoolean("mustChangePassword"))
    }
    suspend fun <T> authenticated(block: suspend () -> T): T {
        return try { block() } catch (e: ApiException) {
            if (e.httpStatus == 401 && e.code == "TOKEN_INVALID") { store.save(refresh()); block() } else throw e
        }
    }
}
