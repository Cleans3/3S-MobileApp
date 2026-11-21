package vn.shelftap.app

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

data class Member(
    val memberId: String,
    val fullName: String,
    val email: String,
    val memberType: String,
    val status: String,
    val historyEnabled: Boolean,
    val mustChangePassword: Boolean
)

class ShelfTapRepository(
    private val sessionStore: SessionStore,
    private val api: ApiClient
) {
    private val refreshMutex = Mutex()

    suspend fun signIn(email: String, password: String): Boolean {
        val response = api.call("POST", "/api/auth/login", JSONObject()
            .put("email", email).put("password", password).put("deviceId", sessionStore.deviceId()))!!
        saveAuth(response)
        return response.optBoolean("mustChangePassword")
    }

    suspend fun hasPendingPasswordChange(): Boolean = sessionStore.read()?.mustChangePassword == true

    suspend fun currentMember(): Member? {
        val session = sessionStore.read() ?: return null
        return try {
            parseMember(api.call("GET", "/api/me", accessToken = session.accessToken)!!)
        } catch (error: ApiException) {
            if (error.status != 401 || error.code != "TOKEN_INVALID") throw error
            refresh()
            val renewed = sessionStore.read() ?: throw ApiException("TOKEN_INVALID", 401)
            if (renewed.mustChangePassword) throw ApiException("PASSWORD_CHANGE_REQUIRED", 403)
            parseMember(api.call("GET", "/api/me", accessToken = renewed.accessToken)!!)
        }
    }

    suspend fun changePassword(current: String, new: String) {
        authenticatedCall("POST", "/api/me/password", JSONObject()
            .put("currentPassword", current).put("newPassword", new))
        val session = sessionStore.read() ?: return
        sessionStore.write(session.copy(mustChangePassword = false))
    }

    suspend fun signOut() {
        try {
            val session = sessionStore.read()
            if (session != null) api.call("POST", "/api/auth/logout", JSONObject().put("refreshToken", session.refreshToken))
        } finally {
            sessionStore.clear()
        }
    }

    private suspend fun authenticatedCall(method: String, path: String, body: JSONObject) {
        val session = sessionStore.read() ?: throw ApiException("TOKEN_INVALID", 401)
        try {
            api.call(method, path, body, session.accessToken)
        } catch (error: ApiException) {
            if (error.status != 401 || error.code != "TOKEN_INVALID") throw error
            refresh()
            val renewed = sessionStore.read() ?: throw ApiException("TOKEN_INVALID", 401)
            api.call(method, path, body, renewed.accessToken)
        }
    }

    private suspend fun refresh() = refreshMutex.withLock {
        val session = sessionStore.read() ?: throw ApiException("TOKEN_INVALID", 401)
        try {
            val response = api.call("POST", "/api/auth/refresh", JSONObject()
                .put("refreshToken", session.refreshToken).put("deviceId", sessionStore.deviceId()))!!
            saveAuth(response)
        } catch (error: ApiException) {
            sessionStore.clear()
            throw error
        }
    }

    private suspend fun saveAuth(response: JSONObject) {
        sessionStore.write(Session(
            accessToken = response.getString("accessToken"),
            refreshToken = response.getString("refreshToken"),
            mustChangePassword = response.optBoolean("mustChangePassword")
        ))
    }

    private fun parseMember(json: JSONObject) = Member(
        json.getString("memberId"), json.getString("fullName"), json.getString("email"),
        json.getString("memberType"), json.getString("status"), json.optBoolean("historyEnabled"),
        json.optBoolean("mustChangePassword")
    )
}
