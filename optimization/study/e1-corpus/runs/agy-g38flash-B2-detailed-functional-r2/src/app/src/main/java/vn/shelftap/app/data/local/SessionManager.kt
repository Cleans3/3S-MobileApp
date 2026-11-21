package vn.shelftap.app.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import vn.shelftap.app.data.model.AuthResponse
import java.util.UUID

/**
 * Represents the authentication state of the application.
 */
sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val mustChangePassword: Boolean) : AuthState
}

/**
 * Manages device identification, persistent user session tokens,
 * and tracks auth state for the app life cycle.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        restoreSession()
    }

    private fun restoreSession() {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        if (!accessToken.isNullOrEmpty() && !refreshToken.isNullOrEmpty()) {
            val mustChange = prefs.getBoolean(KEY_MUST_CHANGE_PASSWORD, false)
            _authState.value = AuthState.Authenticated(mustChangePassword = mustChange)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    /**
     * deviceId is a UUID that the app creates once, on first launch,
     * and keeps for the life of the installation.
     */
    @Synchronized
    fun getDeviceId(): String {
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (deviceId.isNullOrEmpty()) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        return deviceId
    }

    @Synchronized
    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    @Synchronized
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    @Synchronized
    fun isMustChangePassword(): Boolean = prefs.getBoolean(KEY_MUST_CHANGE_PASSWORD, false)

    /**
     * Saves new authentication tokens. Every refresh produces a new single-use
     * refresh token and replaces the old one.
     */
    @Synchronized
    fun saveAuth(authResponse: AuthResponse) {
        val expiresAt = System.currentTimeMillis() + (authResponse.accessExpiresIn * 1000)
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, authResponse.accessToken)
            .putString(KEY_REFRESH_TOKEN, authResponse.refreshToken)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .putBoolean(KEY_MUST_CHANGE_PASSWORD, authResponse.mustChangePassword)
            .apply()

        _authState.value = AuthState.Authenticated(mustChangePassword = authResponse.mustChangePassword)
    }

    /**
     * Updates the mustChangePassword flag after password change or profile update.
     */
    @Synchronized
    fun updateMustChangePassword(mustChange: Boolean) {
        prefs.edit().putBoolean(KEY_MUST_CHANGE_PASSWORD, mustChange).apply()
        if (_authState.value is AuthState.Authenticated) {
            _authState.value = AuthState.Authenticated(mustChangePassword = mustChange)
        }
    }

    /**
     * Clears user session tokens on logout or failed refresh, returning
     * member to sign-in screen, but retains deviceId for the life of installation.
     */
    @Synchronized
    fun clearSession() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_MUST_CHANGE_PASSWORD)
            .apply()

        _authState.value = AuthState.Unauthenticated
    }

    companion object {
        private const val PREFS_NAME = "shelftap_session_prefs"
        private const val KEY_DEVICE_ID = "shelftap_device_id"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_MUST_CHANGE_PASSWORD = "must_change_password"
    }
}
