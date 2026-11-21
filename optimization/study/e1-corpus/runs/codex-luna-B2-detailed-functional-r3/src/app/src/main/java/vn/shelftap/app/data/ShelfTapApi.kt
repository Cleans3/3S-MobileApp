package vn.shelftap.app.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ShelfTapApi(private val store: SessionStore) {
    private val client = OkHttpClient()
    private val jsonType = "application/json".toMediaType()
    private val baseUrl = "https://api.shelftap.example"

    suspend fun login(email: String, password: String): Session = callWithBody("/api/auth/login", "POST", JSONObject().put("email", email).put("password", password).put("deviceId", store.deviceId), false).let(::sessionFrom)
    suspend fun me(): Member = call("/api/me").let { Member(it.getString("memberId"), it.getString("fullName"), it.getString("email"), it.getString("memberType"), it.getString("status"), it.optBoolean("historyEnabled"), it.optBoolean("mustChangePassword")) }
    suspend fun changePassword(current: String, new: String) { callWithBody("/api/me/password", "POST", JSONObject().put("currentPassword", current).put("newPassword", new)); store.save(store.session()!!.copy(mustChangePassword = false)) }
    suspend fun logout() { val refresh = store.session()?.refreshToken ?: return; try { callWithBody("/api/auth/logout", "POST", JSONObject().put("refreshToken", refresh), false) } finally { store.clear() } }

    private suspend fun call(path: String): JSONObject = callWithBody(path, "GET", null)
    private suspend fun callWithBody(path: String, method: String, body: JSONObject?, authenticated: Boolean = true): JSONObject {
        var response = execute(path, method, body, authenticated)
        if (response.code == 401 && authenticated && errorCode(response) == "TOKEN_INVALID") {
            refresh()
            response.close()
            response = execute(path, method, body, true)
        }
        if (!response.isSuccessful) throw failure(response)
        val text = response.body?.string().orEmpty()
        return if (text.isBlank()) JSONObject() else JSONObject(text)
    }
    private suspend fun refresh() {
        val old = store.session()?.refreshToken ?: throw ApiFailure("TOKEN_INVALID", "Please sign in again", 401)
        val response = execute("/api/auth/refresh", "POST", JSONObject().put("refreshToken", old).put("deviceId", store.deviceId), false)
        if (!response.isSuccessful) { store.clear(); throw failure(response) }
        store.save(sessionFrom(JSONObject(response.body?.string().orEmpty())))
        response.close()
    }
    private fun execute(path: String, method: String, body: JSONObject?, authenticated: Boolean): okhttp3.Response {
        val builder = Request.Builder().url(baseUrl + path).header("Accept", "application/json")
        if (authenticated) store.session()?.accessToken?.let { builder.header("Authorization", "Bearer $it") }
        val requestBody = body?.toString()?.toRequestBody(jsonType)
        builder.method(method, requestBody)
        return try { client.newCall(builder.build()).execute() } catch (e: IOException) { throw ApiFailure("NETWORK_ERROR", "Unable to connect. Check your internet connection.", 0) }
    }
    private fun sessionFrom(json: JSONObject) = Session(json.getString("accessToken"), json.getString("refreshToken"), json.optBoolean("mustChangePassword"))
    private fun errorCode(response: okhttp3.Response): String = response.peekBody(1024 * 1024).string().let { runCatching { JSONObject(it).optString("code") }.getOrDefault("") }
    private fun failure(response: okhttp3.Response): ApiFailure { val code = runCatching { JSONObject(response.body?.string().orEmpty()).optString("code") }.getOrDefault(""); val detail = runCatching { JSONObject(response.peekBody(1024 * 1024).string()).optString("detail") }.getOrDefault(""); val retry = response.header("Retry-After")?.toLongOrNull(); response.close(); return ApiFailure(code.ifBlank { "HTTP_${response.code}" }, detail.ifBlank { "Something went wrong." }, response.code, retry) }
}
