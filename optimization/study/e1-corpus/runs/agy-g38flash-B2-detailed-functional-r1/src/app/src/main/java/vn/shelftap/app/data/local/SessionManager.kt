package vn.shelftap.app.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import vn.shelftap.app.data.model.LoginResponse

data class SessionState(
    val isLoggedIn: Boolean,
    val mustChangePassword: Boolean
)

/**
 * Manages access token, refresh token, expiry, and password change requirement.
 * Persists session data in SharedPreferences so signed-in members stay signed in
 * across app launches.
 */
class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _sessionState = MutableStateFlow(loadSessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    // Emits an event when session expires (e.g. refresh token failed)
    private val _sessionExpiredEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpiredEvent: SharedFlow<Unit> = _sessionExpiredEvent.asSharedFlow()

    private fun loadSessionState(): SessionState {
        val hasTokens = !getAccessToken().isNullOrBlank() && !getRefreshToken().isNullOrBlank()
        val mustChange = prefs.getBoolean(KEY_MUST_CHANGE_PASSWORD, false)
        return SessionState(
            isLoggedIn = hasTokens,
            mustChangePassword = mustChange
        )
    }

    @Synchronized
    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    @Synchronized
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    @Synchronized
    fun mustChangePassword(): Boolean = prefs.getBoolean(KEY_MUST_CHANGE_PASSWORD, false)

    @Synchronized
    fun isLoggedIn(): Boolean = !getAccessToken().isNullOrBlank() && !getRefreshToken().isNullOrBlank()

    @Synchronized
    fun saveSession(loginResponse: LoginResponse) {
        val now = System.currentTimeMillis()
        val expiresAt = now + (loginResponse.accessExpiresIn * 1000)
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, loginResponse.accessToken)
            .putString(KEY_REFRESH_TOKEN, loginResponse.refreshToken)
            .putLong(KEY_ACCESS_EXPIRES_AT, expiresAt)
            .putBoolean(KEY_MUST_CHANGE_PASSWORD, loginResponse.mustChangePassword)
            .apply()

        _sessionState.value = SessionState(
            isLoggedIn = true,
            mustChangePassword = loginResponse.mustChangePassword
        )
    }

    @Synchronized
    fun updateTokens(loginResponse: LoginResponse) {
        saveSession(loginResponse)
    }

    @Synchronized
    fun setMustChangePassword(mustChange: Boolean) {
        prefs.edit().putBoolean(KEY_MUST_CHANGE_PASSWORD, mustChange).apply()
        _sessionState.value = _sessionState.value.copy(mustChangePassword = mustChange)
    }

    @Synchronized
    fun clearSession() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_ACCESS_EXPIRES_AT)
            .remove(KEY_MUST_CHANGE_PASSWORD)
            .apply()

        _sessionState.value = SessionState(
            isLoggedIn = false,
            mustChangePassword = false
        )
    }

    fun notifySessionExpired() {
        clearSession()
        _sessionExpiredEvent.tryEmit(Unit)
    }

    companion object {
        private const val PREFS_NAME = "shelftap_session_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_ACCESS_EXPIRES_AT = "access_expires_at"
        private const val KEY_MUST_CHANGE_PASSWORD = "must_change_password"
    }
}
