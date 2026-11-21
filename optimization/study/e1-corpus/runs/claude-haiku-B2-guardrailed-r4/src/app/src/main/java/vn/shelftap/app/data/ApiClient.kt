package vn.shelftap.app.data

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * HTTP layer for the ShelfTap REST API. Stateless: callers pass tokens explicitly.
 * Requests are asynchronous (OkHttp enqueue), so no thread is blocked.
 */
class ApiClient(private val http: OkHttpClient) {

    suspend fun login(email: String, password: String, deviceId: String): TokenPair {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .put("deviceId", deviceId)
        return parseTokens(send(post("/api/auth/login", body)))
    }

    suspend fun refresh(refreshToken: String, deviceId: String): TokenPair {
        val body = JSONObject()
            .put("refreshToken", refreshToken)
            .put("deviceId", deviceId)
        return parseTokens(send(post("/api/auth/refresh", body)))
    }

    suspend fun logout(refreshToken: String) {
        send(post("/api/auth/logout", JSONObject().put("refreshToken", refreshToken)))
    }

    suspend fun me(accessToken: String): Member {
        val request = Request.Builder()
            .url(BASE_URL + "/api/me")
            .get()
            .header(AUTHORIZATION, bearer(accessToken))
        return parseMember(send(request))
    }

    suspend fun changePassword(accessToken: String, currentPassword: String, newPassword: String) {
        val body = JSONObject()
            .put("currentPassword", currentPassword)
            .put("newPassword", newPassword)
        send(post("/api/me/password", body, accessToken))
    }

    private fun post(path: String, body: JSONObject, accessToken: String? = null): Request.Builder {
        val builder = Request.Builder()
            .url(BASE_URL + path)
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
        return if (accessToken != null) builder.header(AUTHORIZATION, bearer(accessToken)) else builder
    }

    /** Returns the response body text for 2xx answers; throws [ApiException] or [NetworkException] otherwise. */
    private suspend fun send(builder: Request.Builder): String = suspendCancellableCoroutine { cont ->
        val okCall = http.newCall(builder.header("Accept", "application/json").build())
        cont.invokeOnCancellation { okCall.cancel() }
        okCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                cont.resumeWithException(NetworkException(e))
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    response.use { resp ->
                        val text = resp.body.string()
                        if (resp.isSuccessful) {
                            cont.resume(text)
                        } else {
                            cont.resumeWithException(
                                parseError(resp.code, resp.header("Retry-After"), text),
                            )
                        }
                    }
                } catch (e: IOException) {
                    cont.resumeWithException(NetworkException(e))
                }
            }
        })
    }

    private fun parseError(httpStatus: Int, retryAfter: String?, body: String): ApiException {
        val json = runCatching { JSONObject(body) }.getOrNull()
        return ApiException(
            code = json?.optString("code")?.ifEmpty { null },
            httpStatus = httpStatus,
            retryAfterSeconds = retryAfter?.trim()?.toIntOrNull(),
        )
    }

    private fun parseTokens(body: String): TokenPair {
        val json = JSONObject(body)
        return TokenPair(
            accessToken = json.getString("accessToken"),
            accessExpiresInSeconds = json.getLong("accessExpiresIn"),
            refreshToken = json.getString("refreshToken"),
            mustChangePassword = json.optBoolean("mustChangePassword", false),
        )
    }

    private fun parseMember(body: String): Member {
        val json = JSONObject(body)
        return Member(
            memberId = json.optString("memberId"),
            fullName = json.optString("fullName"),
            email = json.optString("email"),
            memberType = json.optString("memberType"),
            status = json.optString("status"),
            historyEnabled = json.optBoolean("historyEnabled", false),
            mustChangePassword = json.optBoolean("mustChangePassword", false),
        )
    }

    private fun bearer(token: String) = "Bearer $token"

    companion object {
        const val BASE_URL = "https://api.shelftap.example"
        private const val AUTHORIZATION = "Authorization"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
