package vn.shelftap.app.data.session

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import vn.shelftap.app.data.api.ApiException
import vn.shelftap.app.data.api.MeResponse
import vn.shelftap.app.data.api.SessionEndedException
import vn.shelftap.app.data.api.ShelfTapApi
import vn.shelftap.app.data.api.TokenResponse
import java.util.UUID

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val mustChangePassword: Boolean) : AuthState
}

private const val CODE_TOKEN_INVALID = "TOKEN_INVALID"
private const val CODE_PASSWORD_CHANGE_REQUIRED = "PASSWORD_CHANGE_REQUIRED"

/**
 * Owns the member's session. All reads and writes of the stored state happen under [sessionLock],
 * so concurrent calls never refresh with an already-used (single-use) refresh token.
 */
internal class SessionRepository(
    private val store: SecureStateStore,
    private val api: ShelfTapApi,
    scope: CoroutineScope,
) {
    private val sessionLock = Mutex()

    val authState: StateFlow<AuthState> = store.states
        .map { state ->
            val session = state?.session
            if (session == null) AuthState.SignedOut else AuthState.SignedIn(session.mustChangePassword)
        }
        .stateIn(scope, SharingStarted.Eagerly, AuthState.Loading)

    /** Creates the device ID on first use; it is kept for the life of the installation. */
    suspend fun ensureDeviceId(): String = sessionLock.withLock { currentState().deviceId }

    suspend fun signIn(email: String, password: String) {
        sessionLock.withLock {
            val state = currentState()
            val tokens = api.login(email, password, state.deviceId)
            store.write(state.copy(session = tokens.toStoredSession()))
        }
    }

    /**
     * Tells the server to revoke the refresh token, then removes the session from the phone
     * whether or not the server call succeeded.
     */
    suspend fun signOut() {
        sessionLock.withLock {
            val state = currentState()
            val refreshToken = state.session?.refreshToken
            if (refreshToken != null) {
                try {
                    api.logout(refreshToken)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Server unreachable or rejected: the local session is still removed below.
                }
            }
            store.write(state.copy(session = null))
        }
    }

    suspend fun loadProfile(): MeResponse {
        val me = withAccessToken { token -> api.me(token) }
        updateSession { it.copy(mustChangePassword = me.mustChangePassword) }
        return me
    }

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        withAccessToken { token -> api.changePassword(token, currentPassword, newPassword) }
        updateSession { it.copy(mustChangePassword = false) }
    }

    /**
     * Runs [call] with the current access token. On 401 TOKEN_INVALID the token is refreshed once
     * and the call is repeated. If that also fails, the session ends.
     */
    suspend fun <T> withAccessToken(call: suspend (accessToken: String) -> T): T {
        val token = store.read()?.session?.accessToken ?: throw SessionEndedException()
        try {
            return call(token)
        } catch (e: ApiException) {
            if (e.code == CODE_PASSWORD_CHANGE_REQUIRED) markPasswordChangeRequired()
            if (e.code != CODE_TOKEN_INVALID) throw e
        }

        val refreshed = refreshAfter(token)
        try {
            return call(refreshed)
        } catch (e: ApiException) {
            when (e.code) {
                CODE_TOKEN_INVALID -> {
                    endSession()
                    throw SessionEndedException()
                }
                CODE_PASSWORD_CHANGE_REQUIRED -> markPasswordChangeRequired()
            }
            throw e
        }
    }

    private suspend fun refreshAfter(failedAccessToken: String): String =
        sessionLock.withLock {
            val state = store.read() ?: throw SessionEndedException()
            val session = state.session ?: throw SessionEndedException()
            if (session.accessToken != failedAccessToken) {
                // Another call already refreshed the session while this one was waiting.
                session.accessToken
            } else {
                refreshSession(state, session)
            }
        }

    /** Must be called with [sessionLock] held. */
    private suspend fun refreshSession(state: StoredState, session: StoredSession): String {
        return try {
            val tokens = api.refresh(session.refreshToken, state.deviceId)
            val refreshed = tokens.toStoredSession()
            store.write(state.copy(session = refreshed))
            refreshed.accessToken
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Refresh tokens are single use, so the stored one cannot be trusted after a failure.
            store.write(state.copy(session = null))
            throw SessionEndedException()
        }
    }

    private suspend fun markPasswordChangeRequired() {
        updateSession { it.copy(mustChangePassword = true) }
    }

    private suspend fun endSession() {
        sessionLock.withLock {
            val state = currentState()
            store.write(state.copy(session = null))
        }
    }

    private suspend fun updateSession(transform: (StoredSession) -> StoredSession) {
        sessionLock.withLock {
            val state = store.read() ?: return@withLock
            val session = state.session ?: return@withLock
            store.write(state.copy(session = transform(session)))
        }
    }

    /** Must be called with [sessionLock] held. */
    private suspend fun currentState(): StoredState {
        val existing = store.read()
        if (existing != null && existing.deviceId.isNotBlank()) {
            return existing
        }
        val created = StoredState(deviceId = UUID.randomUUID().toString(), session = existing?.session)
        store.write(created)
        return created
    }

    private fun TokenResponse.toStoredSession(): StoredSession =
        StoredSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            mustChangePassword = mustChangePassword,
        )
}
