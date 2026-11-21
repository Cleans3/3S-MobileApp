package vn.shelftap.app

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

private const val BASE = "https://api.shelftap.example"
class ApiException(val code: String?, val httpStatus: Int, val retryAfter: Long? = null, message: String): IOException(message)
class Api(private val store: SessionStore) {
    private val client = OkHttpClient()
    private val refreshLock = Mutex()
    private fun bodyJson(body: String) = JSONObject(body)
    private fun error(response: Response): ApiException { val raw=response.body?.string().orEmpty(); val j=runCatching { bodyJson(raw) }.getOrNull(); val retry=response.header("Retry-After")?.toLongOrNull(); return ApiException(j?.optString("code")?.takeIf { it.isNotBlank() }, response.code, retry, j?.optString("detail")?.takeIf { it.isNotBlank() } ?: "Request failed") }
    private suspend fun request(path: String, method: String, json: JSONObject? = null, authenticated: Boolean = false, retry: Boolean = true): Response {
        val builder=Request.Builder().url(BASE+path).header("Accept","application/json")
        if (json != null) builder.method(method, json.toString().toRequestBody("application/json".toMediaType())) else builder.method(method, null)
        if (authenticated) store.session()?.accessToken?.let { builder.header("Authorization", "Bearer $it") }
        val response=client.newCall(builder.build()).execute()
        if (response.isSuccessful) return response
        val ex=error(response); response.close()
        if (authenticated && ex.httpStatus == 401 && ex.code == "TOKEN_INVALID" && retry) {
            if (refresh()) return request(path,method,json,true,false)
        }
        throw ex
    }
    private suspend fun refresh(): Boolean = refreshLock.withLock {
        val old=store.session()?.refreshToken ?: return false
        return try { val j=JSONObject().put("refreshToken",old).put("deviceId",store.deviceId()); val response=request("/api/auth/refresh","POST",j,false,false); val out=bodyJson(response.body!!.string()); store.save(Session(out.getString("accessToken"),out.getString("refreshToken"),out.optBoolean("mustChangePassword"))); true } catch (_: Exception) { store.clear(); false }
    }
    suspend fun login(email: String,password: String): Session { val j=JSONObject().put("email",email).put("password",password).put("deviceId",store.deviceId()); val r=request("/api/auth/login","POST",j,false,false); val o=bodyJson(r.body!!.string()); return Session(o.getString("accessToken"),o.getString("refreshToken"),o.optBoolean("mustChangePassword")) }
    suspend fun me(): Member { val r=request("/api/me","GET",authenticated=true); val o=bodyJson(r.body!!.string()); return Member(o.getString("memberId"),o.getString("fullName"),o.getString("email"),o.getString("memberType"),o.getString("status"),o.optBoolean("historyEnabled"),o.optBoolean("mustChangePassword")) }
    suspend fun changePassword(current: String,new: String) { val j=JSONObject().put("currentPassword",current).put("newPassword",new); request("/api/me/password","POST",j,true).close(); store.setMustChange(false) }
    suspend fun logout() { val r=store.session(); try { if (r != null) request("/api/auth/logout","POST",JSONObject().put("refreshToken",r.refreshToken),false,false).close() } finally { store.clear() } }
}
