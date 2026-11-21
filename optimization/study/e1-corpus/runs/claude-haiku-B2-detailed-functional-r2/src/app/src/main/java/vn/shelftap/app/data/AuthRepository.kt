package vn.shelftap.app.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Owns the member session: sign-in, sign-out, authorised calls and token refresh.
 *
 * Refresh tokens are single use, so every refresh is serialised behind [refreshMutex].
 */
class AuthRepository(
    private val api: ApiClient,
    private val session: SessionStore,
    private val json: Json,
) {
    private val refreshMutex = Mutex()

    suspend fun hasSession(): Boolean = session.readTokens() != null

    /** Signs in and stores the new session. Failed sign-ins leave any existing session untouched. */
    suspend fun signIn(email: String, password: String): TokenResponse {
        val body = json.encodeToString(LoginRequest(email, password, session.deviceId()))
        val response = json.decodeFromString<TokenResponse>(
            api.send("POST", "/api/auth/login", jsonBody = body),
        )
        session.saveTokens(Tokens(response.accessToken, response.refreshToken))
        return response
    }

    suspend fun me(): MemberDto =
        json.decodeFromString<MemberDto>(authorized("GET", "/api/me"))

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        val body = json.encodeToString(ChangePasswordRequest(currentPassword, newPassword))
        authorized("POST", "/api/me/password", body)
    }

    /**
     * Tells the server to end the session, then removes it from the phone.
     * The local session is removed even if the server cannot be reached.
     */
    suspend fun signOut() {
        try {
            val tokens = session.readTokens()
            if (tokens != null) {
                val body = json.encodeToString(LogoutRequest(tokens.refreshToken))
                api.send("POST", "/api/auth/logout", jsonBody = body)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Server unreachable or already revoked: the local session is still removed below.
        } finally {
            session.clearSession()
        }
    }

    /**
     * Performs an authorised call. On 401 TOKEN_INVALID it refreshes the token once and repeats
     * the call. If the refresh fails, the session is cleared and [SessionExpiredException] is thrown.
     */
    private suspend fun authorized(method: String, path: String, jsonBody: String? = null): String {
        val tokens = session.readTokens() ?: throw SessionExpiredException()
        return try {
            api.send(method, path, bearer = tokens.accessToken, jsonBody = jsonBody)
        } catch (e: ApiException) {
            if (!e.isTokenInvalid) throw e
            val freshAccessToken = refreshAfter(tokens.accessToken)
            try {
                api.send(method, path, bearer = freshAccessToken, jsonBody = jsonBody)
            } catch (retry: ApiException) {
                if (retry.isTokenInvalid) {
                    session.clearSession()
                    throw SessionExpiredException()
                }
                throw retry
            }
        }
    }

    /**
     * Refreshes the session after a call failed with [staleAccessToken].
     * If another call already refreshed while this one waited for the lock, its result is reused,
     * so the single-use refresh token is never spent twice.
     */
    private suspend fun refreshAfter(staleAccessToken: String): String = refreshMutex.withLock {
        val current = session.readTokens() ?: throw SessionExpiredException()
        if (current.accessToken != staleAccessToken) {
            return@withLock current.accessToken
        }

        val body = json.encodeToString(RefreshRequest(current.refreshToken, session.deviceId()))
        val response = try {
            json.decodeFromString<TokenResponse>(
                api.send("POST", "/api/auth/refresh", jsonBody = body),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The refresh failed, so the member goes back to sign-in.
            session.clearSession()
            throw SessionExpiredException()
        }

        // The old refresh token is now spent. Store the new pair before anything else uses it.
        session.saveTokens(Tokens(response.accessToken, response.refreshToken))
        response.accessToken
    }
}
