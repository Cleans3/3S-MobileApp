package vn.shelftap.app.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

/**
 * Sign-in, sign-out, profile and password use cases. Refresh tokens are single use, so token
 * refreshes are serialised: a second caller that was rejected with the same stale token reuses
 * the token that the first caller obtained instead of spending the (now used) refresh token again.
 */
class AccountRepository(
    private val api: ShelfTapApi,
    private val store: SessionStore,
    private val sessions: SessionManager,
) {
    private val refreshLock = Mutex()

    suspend fun signIn(email: String, password: String) {
        val deviceId = store.deviceId()
        val tokens = api.login(email, password, deviceId)
        sessions.save(StoredSession(tokens.accessToken, tokens.refreshToken, tokens.mustChangePassword))
    }

    /**
     * Tells the server, then removes the session from the phone. The local removal happens even
     * if the server cannot be reached or rejects the call.
     */
    suspend fun signOut() {
        val session = sessions.currentSession()
        if (session != null) {
            try {
                api.logout(session.refreshToken)
            } catch (e: IOException) {
                // Network failure: the local sign-out below still applies.
            } catch (e: ApiException) {
                // Server rejected the logout: the local sign-out below still applies.
            }
        }
        sessions.clear()
    }

    suspend fun loadProfile(): Member {
        val member = withAccessToken { token -> api.me(token) }
        if (member.mustChangePassword) {
            sessions.markPasswordChangeRequired()
        }
        return member
    }

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        withAccessToken { token -> api.changePassword(token, currentPassword, newPassword) }
        sessions.markPasswordChanged()
    }

    /**
     * Runs [call] with the stored access token. On TOKEN_INVALID it refreshes once and repeats
     * the call. If the refresh fails, the session is cleared and [SessionEndedException] is thrown.
     */
    private suspend fun <T> withAccessToken(call: suspend (String) -> T): T {
        val session = sessions.currentSession() ?: throw SessionEndedException()
        return try {
            call(session.accessToken)
        } catch (e: ApiException) {
            when (e.code) {
                "TOKEN_INVALID" -> call(refreshAfterRejection(session.accessToken))
                "PASSWORD_CHANGE_REQUIRED" -> {
                    sessions.markPasswordChangeRequired()
                    throw e
                }
                else -> throw e
            }
        }
    }

    private suspend fun refreshAfterRejection(rejectedAccessToken: String): String =
        refreshLock.withLock {
            val current = sessions.currentSession() ?: throw SessionEndedException()
            if (current.accessToken != rejectedAccessToken) {
                // Another call already refreshed the session while this one waited.
                return@withLock current.accessToken
            }
            try {
                val tokens = api.refresh(current.refreshToken, store.deviceId())
                val updated = StoredSession(tokens.accessToken, tokens.refreshToken, tokens.mustChangePassword)
                sessions.save(updated)
                updated.accessToken
            } catch (e: ApiException) {
                // The server refused the refresh token: the member must sign in again.
                sessions.clear()
                throw SessionEndedException()
            }
        }
}
