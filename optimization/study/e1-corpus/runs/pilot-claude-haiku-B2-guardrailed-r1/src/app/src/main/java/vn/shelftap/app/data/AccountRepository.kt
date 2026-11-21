package vn.shelftap.app.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

data class Member(
    val memberId: String,
    val fullName: String,
    val email: String,
    val memberType: String,
    val status: String,
    val historyEnabled: Boolean,
    val mustChangePassword: Boolean,
)

/** Thrown when the session cannot be renewed. The stored session has already been removed. */
class SessionEndedException : Exception("ShelfTap session ended")

/**
 * Account operations. Every suspend function moves its blocking work onto [io],
 * so it is safe to call from the main thread.
 */
class AccountRepository(
    private val api: ApiClient,
    private val store: SessionStore,
    private val io: CoroutineDispatcher,
) {
    /** Serialises refreshes: refresh tokens are single use, so only one refresh may run at a time. */
    private val refreshLock = Mutex()

    suspend fun hasSession(): Boolean = withContext(io) {
        store.tokens() != null
    }

    /** Signs in and stores the tokens. Returns the mustChangePassword flag from the login response. */
    suspend fun signIn(email: String, password: String): Boolean = withContext(io) {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .put("deviceId", store.deviceId())
        val json = JSONObject(api.send("POST", "/api/auth/login", null, body))
        store.saveTokens(json.toTokens())
        json.optBoolean("mustChangePassword", false)
    }

    suspend fun currentMember(): Member = authorized { token ->
        parseMember(JSONObject(api.send("GET", "/api/me", token, null)))
    }

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        val body = JSONObject()
            .put("currentPassword", currentPassword)
            .put("newPassword", newPassword)
        authorized { token ->
            api.send("POST", "/api/me/password", token, body)
            Unit
        }
    }

    /** Tells the server to end the session, then removes it from the phone even if the call fails. */
    suspend fun signOut(): Unit = withContext(io) {
        val refresh = store.tokens()?.refreshToken
        try {
            if (refresh != null) {
                api.send("POST", "/api/auth/logout", null, JSONObject().put("refreshToken", refresh))
            }
        } catch (e: IOException) {
            // Network failure: the local session is still removed below.
        } catch (e: ApiException) {
            // Server rejected the logout: the local session is still removed below.
        } finally {
            store.clearTokens()
        }
    }

    /**
     * Runs [block] with the stored access token. On TOKEN_INVALID it refreshes once and repeats
     * the call. If the refresh fails, or the repeated call is still rejected, the session ends.
     */
    private suspend fun <T> authorized(block: (String) -> T): T {
        val first = withContext(io) { store.tokens()?.accessToken } ?: throw SessionEndedException()
        try {
            return withContext(io) { block(first) }
        } catch (e: ApiException) {
            if (e.code != "TOKEN_INVALID") throw e
        }

        val fresh = refreshAfter(first)
        return withContext(io) {
            try {
                block(fresh)
            } catch (e: ApiException) {
                if (e.code == "TOKEN_INVALID") {
                    store.clearTokens()
                    throw SessionEndedException()
                }
                throw e
            }
        }
    }

    /**
     * Refreshes the tokens unless another call already replaced [staleAccessToken].
     * Throws [SessionEndedException] after clearing the session if the refresh fails.
     */
    private suspend fun refreshAfter(staleAccessToken: String): String = refreshLock.withLock {
        val current = withContext(io) { store.tokens() } ?: throw SessionEndedException()
        if (current.accessToken != staleAccessToken) {
            return@withLock current.accessToken
        }
        try {
            val fresh = withContext(io) {
                val body = JSONObject()
                    .put("refreshToken", current.refreshToken)
                    .put("deviceId", store.deviceId())
                val tokens = JSONObject(api.send("POST", "/api/auth/refresh", null, body)).toTokens()
                store.saveTokens(tokens)
                tokens
            }
            fresh.accessToken
        } catch (e: IOException) {
            endSession()
            throw SessionEndedException()
        } catch (e: ApiException) {
            endSession()
            throw SessionEndedException()
        } catch (e: JSONException) {
            endSession()
            throw SessionEndedException()
        }
    }

    private suspend fun endSession() {
        withContext(io) { store.clearTokens() }
    }

    private fun JSONObject.toTokens(): Tokens = Tokens(
        accessToken = getString("accessToken"),
        refreshToken = getString("refreshToken"),
    )

    private fun parseMember(json: JSONObject): Member = Member(
        memberId = json.optString("memberId"),
        fullName = json.optString("fullName"),
        email = json.optString("email"),
        memberType = json.optString("memberType"),
        status = json.optString("status"),
        historyEnabled = json.optBoolean("historyEnabled", false),
        mustChangePassword = json.optBoolean("mustChangePassword", false),
    )
}
