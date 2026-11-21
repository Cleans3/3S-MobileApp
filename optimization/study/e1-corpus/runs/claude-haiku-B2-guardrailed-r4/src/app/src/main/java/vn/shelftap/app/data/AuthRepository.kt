package vn.shelftap.app.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val mustChangePassword: Boolean) : AuthState
}

/**
 * Owns the session: sign-in, sign-out, token refresh and the "must change password" rule.
 *
 * Refresh tokens are single use, so every refresh is serialised through [lock] and the rotated
 * refresh token is persisted before the call returns.
 */
class AuthRepository(
    private val api: ApiClient,
    private val store: SessionStore,
    private val newDeviceId: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val lock = Mutex()
    private var session: Session? = null // guarded by lock
    private var restored = false // guarded by lock

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /** Loads the stored session without any network call, so a signed-in member stays signed in. */
    suspend fun restore() {
        val current = lock.withLock {
            if (restored) return
            restored = true
            loadLocked()
        }
        _authState.value = if (current.refreshToken == null) {
            AuthState.SignedOut
        } else {
            AuthState.SignedIn(current.mustChangePassword)
        }
    }

    suspend fun signIn(email: String, password: String) {
        val deviceId = lock.withLock { loadLocked().deviceId }
        val pair = api.login(email, password, deviceId)
        lock.withLock {
            persistLocked(
                Session(
                    deviceId = deviceId,
                    accessToken = pair.accessToken,
                    accessExpiresAtMillis = expiresAt(pair.accessExpiresInSeconds),
                    refreshToken = pair.refreshToken,
                    mustChangePassword = pair.mustChangePassword,
                ),
            )
        }
        _authState.value = AuthState.SignedIn(pair.mustChangePassword)
    }

    suspend fun fetchMe(): Member {
        val member = authorized { token -> api.me(token) }
        lock.withLock {
            persistLocked(loadLocked().copy(mustChangePassword = member.mustChangePassword))
        }
        _authState.value = AuthState.SignedIn(member.mustChangePassword)
        return member
    }

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        authorized { token -> api.changePassword(token, currentPassword, newPassword) }
        lock.withLock {
            persistLocked(loadLocked().copy(mustChangePassword = false))
        }
        _authState.value = AuthState.SignedIn(false)
    }

    /**
     * Tells the server (best effort) and always forgets the session on this phone.
     * The device id is kept so the installation keeps the same identity.
     */
    suspend fun signOut() {
        val refreshToken = lock.withLock { loadLocked().refreshToken }
        if (refreshToken != null) {
            try {
                api.logout(refreshToken)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The server could not be reached; the local session is removed regardless.
            }
        }
        lock.withLock { clearTokensLocked() }
    }

    /**
     * Runs [call] with a valid access token. On TOKEN_INVALID it refreshes once and repeats the call.
     * On PASSWORD_CHANGE_REQUIRED it switches the app to the forced password change screen.
     */
    private suspend fun <T> authorized(call: suspend (String) -> T): T {
        val snapshot = lock.withLock { loadLocked() }
        val needsRenewal = snapshot.accessToken == null ||
            clock() >= snapshot.accessExpiresAtMillis - REFRESH_MARGIN_MILLIS
        if (needsRenewal) renewAccessToken(failedToken = null)

        val token = currentAccessToken()
        return try {
            call(token)
        } catch (e: ApiException) {
            when (e.code) {
                "TOKEN_INVALID" -> {
                    renewAccessToken(failedToken = token)
                    call(currentAccessToken())
                }
                "PASSWORD_CHANGE_REQUIRED" -> {
                    markMustChangePassword()
                    throw e
                }
                else -> throw e
            }
        }
    }

    private suspend fun currentAccessToken(): String = lock.withLock {
        loadLocked().accessToken ?: throw SignedOutException()
    }

    /**
     * Refreshes the access token. If [failedToken] is no longer the current one, another caller
     * already refreshed, so nothing is sent. Any failure ends the session.
     */
    private suspend fun renewAccessToken(failedToken: String?) {
        lock.withLock {
            val current = loadLocked()
            if (failedToken != null && current.accessToken != failedToken) return
            val refreshToken = current.refreshToken ?: run {
                clearTokensLocked()
                throw SignedOutException()
            }
            try {
                val pair = api.refresh(refreshToken, current.deviceId)
                persistLocked(
                    current.copy(
                        accessToken = pair.accessToken,
                        accessExpiresAtMillis = expiresAt(pair.accessExpiresInSeconds),
                        refreshToken = pair.refreshToken,
                        mustChangePassword = pair.mustChangePassword,
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                clearTokensLocked()
                throw SignedOutException()
            }
        }
    }

    private suspend fun markMustChangePassword() {
        lock.withLock {
            persistLocked(loadLocked().copy(mustChangePassword = true))
        }
        _authState.value = AuthState.SignedIn(true)
    }

    private suspend fun clearTokensLocked() {
        val current = loadLocked()
        persistLocked(Session(deviceId = current.deviceId))
        _authState.value = AuthState.SignedOut
    }

    private suspend fun loadLocked(): Session {
        session?.let { return it }
        val loaded = store.load() ?: Session(deviceId = newDeviceId()).also { store.save(it) }
        session = loaded
        return loaded
    }

    private suspend fun persistLocked(updated: Session) {
        store.save(updated)
        session = updated
    }

    private fun expiresAt(seconds: Long): Long = clock() + seconds * 1000L

    private companion object {
        const val REFRESH_MARGIN_MILLIS = 30_000L
    }
}
