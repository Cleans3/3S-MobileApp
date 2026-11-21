package vn.shelftap.app.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.util.UUID

/**
 * Sign-in, token refresh and sign-out. Refresh tokens are single use, so every
 * read-modify-write of the session runs under [mutex] to avoid racing refreshes.
 */
class AuthRepository(
    private val api: ApiClient,
    private val store: EncryptedSessionStore,
) {
    private val mutex = Mutex()

    /** Also creates the device ID on first launch. */
    suspend fun hasSession(): Boolean = mutex.withLock { ensureStored().refreshToken != null }

    /** Returns true when the member must change the password before anything else. */
    suspend fun signIn(email: String, password: String): Boolean {
        val deviceId = mutex.withLock { ensureStored().deviceId }
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .put("deviceId", deviceId)
        val tokens = parseTokens(api.post(PATH_LOGIN, body))
        mutex.withLock {
            val current = ensureStored()
            store.write(current.copy(accessToken = tokens.accessToken, refreshToken = tokens.refreshToken))
        }
        return tokens.mustChangePassword
    }

    /** Tells the server, then removes the session from the phone even if the call failed. */
    suspend fun signOut() {
        val refreshToken = mutex.withLock { ensureStored().refreshToken }
        if (refreshToken != null) {
            try {
                api.post(PATH_LOGOUT, JSONObject().put("refreshToken", refreshToken))
            } catch (e: RemoteException) {
                // Ignored on purpose: the local session is removed below regardless.
            }
        }
        endSession()
    }

    suspend fun fetchMe(): Member = authenticated { token ->
        parseMember(api.get(PATH_ME, token))
    }

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        authenticated { token ->
            api.post(
                PATH_PASSWORD,
                JSONObject()
                    .put("currentPassword", currentPassword)
                    .put("newPassword", newPassword),
                token,
            )
        }
    }

    /**
     * Runs [call] with the stored access token. On TOKEN_INVALID it refreshes once and
     * repeats the call. If the refresh or the repeat fails, the session ends.
     */
    private suspend fun <T> authenticated(call: suspend (String) -> T): T {
        val token = store.read()?.accessToken ?: throw SessionEndedException()
        return try {
            call(token)
        } catch (first: ApiException) {
            if (first.code != ErrorCodes.TOKEN_INVALID) throw first
            val fresh = refreshAccessToken(staleAccessToken = token)
            try {
                call(fresh)
            } catch (retry: ApiException) {
                if (retry.code == ErrorCodes.TOKEN_INVALID) {
                    endSession()
                    throw SessionEndedException()
                }
                throw retry
            }
        }
    }

    private suspend fun refreshAccessToken(staleAccessToken: String): String = mutex.withLock {
        val current = ensureStored()

        // Another call may have refreshed while this one waited for the lock.
        val latest = current.accessToken
        if (latest != null && latest != staleAccessToken) return@withLock latest

        val refreshToken = current.refreshToken ?: throw SessionEndedException()
        val tokens = try {
            parseTokens(
                api.post(
                    PATH_REFRESH,
                    JSONObject()
                        .put("refreshToken", refreshToken)
                        .put("deviceId", current.deviceId),
                ),
            )
        } catch (e: RemoteException) {
            store.write(current.copy(accessToken = null, refreshToken = null))
            throw SessionEndedException()
        }

        // The refresh token rotates: the new one replaces the old one.
        store.write(current.copy(accessToken = tokens.accessToken, refreshToken = tokens.refreshToken))
        tokens.accessToken
    }

    private suspend fun endSession() {
        mutex.withLock {
            val current = ensureStored()
            store.write(current.copy(accessToken = null, refreshToken = null))
        }
    }

    /** Must be called while holding [mutex]. */
    private suspend fun ensureStored(): StoredSession =
        store.read() ?: StoredSession(deviceId = UUID.randomUUID().toString()).also { store.write(it) }

    private fun parseTokens(json: JSONObject): TokenPair {
        val access = json.optString("accessToken")
        val refresh = json.optString("refreshToken")
        if (access.isEmpty() || refresh.isEmpty()) {
            throw ApiException(httpStatus = 200, code = ErrorCodes.INVALID_RESPONSE)
        }
        return TokenPair(
            accessToken = access,
            refreshToken = refresh,
            mustChangePassword = json.optBoolean("mustChangePassword", false),
        )
    }

    private fun parseMember(json: JSONObject): Member = Member(
        memberId = json.optString("memberId"),
        fullName = json.optString("fullName"),
        email = json.optString("email"),
        memberType = json.optString("memberType"),
        status = json.optString("status"),
        historyEnabled = json.optBoolean("historyEnabled", false),
        mustChangePassword = json.optBoolean("mustChangePassword", false),
    )

    private companion object {
        const val PATH_LOGIN = "/api/auth/login"
        const val PATH_REFRESH = "/api/auth/refresh"
        const val PATH_LOGOUT = "/api/auth/logout"
        const val PATH_ME = "/api/me"
        const val PATH_PASSWORD = "/api/me/password"
    }
}
