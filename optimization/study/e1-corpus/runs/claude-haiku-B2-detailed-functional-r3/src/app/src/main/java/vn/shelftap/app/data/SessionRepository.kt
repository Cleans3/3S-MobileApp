package vn.shelftap.app.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SessionRepository(
    private val api: ShelfTapApi,
    private val store: SessionStore,
) {

    /** Serialises refreshes so two concurrent 401s never spend the same single-use refresh token. */
    private val refreshLock = Mutex()

    /** Ensures the device id exists (first launch) and reports whether a session is stored. */
    suspend fun initialize(): Boolean {
        store.ensureDeviceId()
        return store.read().refreshToken != null
    }

    suspend fun signIn(email: String, password: String): TokenResponse {
        val deviceId = store.ensureDeviceId()
        val tokens = api.login(email, password, deviceId)
        store.saveTokens(tokens.accessToken, tokens.refreshToken)
        return tokens
    }

    suspend fun me(): MeResponse = authorized { token -> api.me(token) }

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        authorized { token -> api.changePassword(token, currentPassword, newPassword) }
    }

    /** Tells the server, then always removes the session from this phone. */
    suspend fun signOut() {
        val refreshToken = store.read().refreshToken
        if (refreshToken != null) {
            runCatching { api.logout(refreshToken) }
        }
        store.clearSession()
    }

    /**
     * Runs [call] with the stored access token. On 401 TOKEN_INVALID it refreshes once and
     * repeats the call. If the refresh fails, the session is cleared and [SessionEndedException] is thrown.
     */
    private suspend fun <T> authorized(call: suspend (accessToken: String) -> T): T {
        val used = store.read().accessToken
        if (used != null) {
            try {
                return call(used)
            } catch (e: ApiException) {
                if (!e.isTokenInvalid) throw e
            }
        }
        return call(refreshAfter(used))
    }

    private suspend fun refreshAfter(failedToken: String?): String = refreshLock.withLock {
        val current = store.read()
        val latest = current.accessToken
        // Another call already refreshed while we waited for the lock; use its result.
        if (latest != null && latest != failedToken) return@withLock latest

        val refreshToken = current.refreshToken ?: throw SessionEndedException()
        try {
            val tokens = api.refresh(refreshToken, store.ensureDeviceId())
            store.saveTokens(tokens.accessToken, tokens.refreshToken)
            tokens.accessToken
        } catch (e: ApiException) {
            store.clearSession()
            throw SessionEndedException()
        }
    }
}
