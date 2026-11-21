package vn.shelftap.app.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

sealed interface AuthState {
    /** Startup, before the stored session has been read. */
    data object Loading : AuthState

    data object SignedOut : AuthState

    /** [mustChangePassword] true means the member has to change the password before anything else. */
    data class SignedIn(val mustChangePassword: Boolean) : AuthState
}

/**
 * Owns the member session: sign in/out, token refresh with single-flight locking,
 * and the authorized-call wrapper that retries once after a TOKEN_INVALID response.
 */
class SessionRepository(
    private val api: ApiClient,
    private val store: SessionStore,
) {
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    /** Serialises refreshes so two concurrent calls cannot both spend the same single-use refresh token. */
    private val refreshLock = Mutex()

    init {
        // A stored refresh token means the member stays signed in across app restarts.
        _state.value = if (store.refreshToken != null) {
            AuthState.SignedIn(store.mustChangePassword)
        } else {
            AuthState.SignedOut
        }
    }

    /** Throws [ApiException] on a rejected sign-in, IOException on network failure. */
    suspend fun signIn(email: String, password: String) {
        val tokens = api.login(email, password, store.deviceId())
        applyTokens(tokens)
    }

    /** Tells the server, then always removes the session from this phone. */
    suspend fun signOut() {
        val refresh = store.refreshToken
        if (refresh != null) {
            try {
                api.logout(refresh)
            } catch (e: ApiException) {
                // Ignored: the local session is removed regardless.
            } catch (e: IOException) {
                // Ignored: the local session is removed regardless.
            }
        }
        endSession()
    }

    /** Loads /api/me as an authorized call. */
    suspend fun loadProfile(): MeResponse {
        val me = authorized { token -> api.me(token) }
        if (me.mustChangePassword) requirePasswordChange()
        return me
    }

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        authorized { token -> api.changePassword(token, currentPassword, newPassword) }
        store.setMustChangePassword(false)
        _state.value = AuthState.SignedIn(false)
    }

    /** Sends the member to the forced change-password screen. */
    fun requirePasswordChange() {
        if (store.refreshToken == null) return
        store.setMustChangePassword(true)
        _state.value = AuthState.SignedIn(true)
    }

    /**
     * Runs [call] with the current access token. On TOKEN_INVALID it refreshes once and
     * repeats the call. If the refresh or the repeated call is rejected, the session ends.
     */
    suspend fun <T> authorized(call: suspend (accessToken: String) -> T): T {
        val current = store.accessToken ?: throw SessionEndedException()
        try {
            return call(current)
        } catch (e: ApiException) {
            if (e.code == "PASSWORD_CHANGE_REQUIRED") {
                requirePasswordChange()
                throw e
            }
            if (e.code != "TOKEN_INVALID") throw e
        }

        val fresh = refreshAccessToken(staleToken = current)
        try {
            return call(fresh)
        } catch (e: ApiException) {
            when (e.code) {
                "PASSWORD_CHANGE_REQUIRED" -> {
                    requirePasswordChange()
                    throw e
                }
                "TOKEN_INVALID" -> {
                    endSession()
                    throw SessionEndedException()
                }
                else -> throw e
            }
        }
    }

    private suspend fun refreshAccessToken(staleToken: String): String = refreshLock.withLock {
        // Another call may already have refreshed while this one waited for the lock.
        val latest = store.accessToken
        if (latest != null && latest != staleToken) return@withLock latest

        val refresh = store.refreshToken ?: throw SessionEndedException()
        try {
            val tokens = api.refresh(refresh, store.deviceId())
            applyTokens(tokens)
            tokens.accessToken
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The refresh failed, so the member goes back to sign-in.
            endSession()
            throw SessionEndedException()
        }
    }

    private fun applyTokens(tokens: TokenResponse) {
        store.saveTokens(tokens.accessToken, tokens.refreshToken, tokens.mustChangePassword)
        _state.value = AuthState.SignedIn(tokens.mustChangePassword)
    }

    private fun endSession() {
        store.clearSession()
        _state.value = AuthState.SignedOut
    }
}
