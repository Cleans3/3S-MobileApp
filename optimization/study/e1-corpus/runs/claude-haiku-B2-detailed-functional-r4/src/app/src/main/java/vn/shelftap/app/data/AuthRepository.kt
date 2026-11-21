package vn.shelftap.app.data

import java.io.IOException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Session logic: sign-in, sign-out, and authorized calls that refresh the
 * access token once on TOKEN_INVALID.
 */
class AuthRepository(
    private val api: ShelfTapApi,
    private val store: SessionStore,
) {

    /** Serializes refreshes so two requests never spend the same single-use refresh token. */
    private val refreshLock = Mutex()

    val hasSession: Boolean
        get() = store.refreshToken != null

    suspend fun login(email: String, password: String): TokenResponse {
        val tokens = api.login(email, password, store.deviceId)
        store.save(tokens.accessToken, tokens.refreshToken)
        return tokens
    }

    /** Tells the server, then removes the session from the phone even if the server call fails. */
    suspend fun logout() {
        val refresh = store.refreshToken
        try {
            if (refresh != null) api.logout(refresh)
        } catch (e: ApiException) {
            // Server rejected the call: the phone still signs out below.
        } catch (e: IOException) {
            // Server unreachable: the phone still signs out below.
        } finally {
            store.clear()
        }
    }

    suspend fun me(): Me = authorized { token -> api.me(token) }

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        authorized { token -> api.changePassword(token, currentPassword, newPassword) }
    }

    /**
     * Runs [call] with the stored access token. On 401 TOKEN_INVALID it refreshes
     * once and repeats the call. A failed refresh throws [SessionEndedException].
     */
    private suspend fun <T> authorized(call: suspend (accessToken: String) -> T): T {
        val token = store.accessToken ?: throw SessionEndedException()
        try {
            return call(token)
        } catch (e: ApiException) {
            if (e.status != 401 || e.code != "TOKEN_INVALID") throw e
        }
        refreshAfter(staleAccessToken = token)
        val fresh = store.accessToken ?: throw SessionEndedException()
        return call(fresh)
    }

    private suspend fun refreshAfter(staleAccessToken: String) {
        refreshLock.withLock {
            val current = store.accessToken
            // Another request already refreshed while we waited for the lock.
            if (current != null && current != staleAccessToken) return

            val refresh = store.refreshToken ?: throw SessionEndedException()
            try {
                val tokens = withContext(NonCancellable) {
                    api.refresh(refresh, store.deviceId)
                }
                store.save(tokens.accessToken, tokens.refreshToken)
            } catch (e: Exception) {
                // The refresh token is single use, so a failed attempt leaves nothing to reuse.
                store.clear()
                throw SessionEndedException()
            }
        }
    }
}
