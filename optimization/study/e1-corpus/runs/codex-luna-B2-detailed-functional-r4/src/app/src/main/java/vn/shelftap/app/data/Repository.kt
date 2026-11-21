package vn.shelftap.app.data

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class Repository(private val api: ShelfTapApi, private val session: SessionStore) {
    private val gson = Gson()

    suspend fun login(email: String, password: String): Member = withContext(Dispatchers.IO) {
        val result = api.login(LoginRequest(email, password, session.deviceId)).checked()
        session.saveAuth(result)
        runCatching { api.me().checked() }.getOrElse { error ->
            if (result.mustChangePassword) Member("", "", email, "", "", false, true)
            else throw error
        }
    }

    suspend fun currentMember(): Member = withContext(Dispatchers.IO) { api.me().checked() }

    suspend fun changePassword(current: String, new: String) = withContext(Dispatchers.IO) {
        api.changePassword(PasswordRequest(current, new)).checkedVoid()
        session.setMustChangePassword(false)
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        val token = session.refreshToken()
        try { if (token != null) api.logout(LogoutRequest(token)) } finally { session.clear() }
    }

    fun hasSession() = session.accessToken() != null && session.refreshToken() != null
    fun mustChangePassword() = session.mustChangePassword()
    fun clearSession() = session.clear()

    private fun <T> Response<T>.checked(): T {
        if (isSuccessful) return body() as T
        val parsed = errorBody()?.string()?.let { runCatching { gson.fromJson(it, ApiError::class.java) }.getOrNull() }
        throw ShelfTapException(withRetryAfter(parsed ?: ApiError(status = code(), detail = "Unable to complete the request")))
    }

    private fun Response<Void>.checkedVoid() {
        if (isSuccessful) return
        val parsed = errorBody()?.string()?.let { runCatching { gson.fromJson(it, ApiError::class.java) }.getOrNull() }
        throw ShelfTapException(withRetryAfter(parsed ?: ApiError(status = code(), detail = "Unable to complete the request")))
    }

    private fun <T> Response<T>.withRetryAfter(error: ApiError): ApiError {
        if (error.code == "RATE_LIMITED" || code() == 429) {
            val seconds = headers()["Retry-After"]
            return error.copy(detail = (error.detail ?: "Too many requests") + (seconds?.let { " Try again in $it seconds." } ?: ""))
        }
        return error
    }
}
