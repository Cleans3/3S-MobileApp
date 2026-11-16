package shelftap.android.session

/** Why the app dropped the session by itself (shown once on the login screen). */
enum class LogoutReason { SESSION_EXPIRED, TOKEN_REUSED, INACTIVITY, LOCK_KEY_INVALIDATED }

sealed interface SessionState {
    /** App start, before the stored refresh token has been looked at. */
    data object Loading : SessionState
    data class LoggedOut(val reason: LogoutReason? = null) : SessionState

    /** A refresh token is stored but the server could not be reached; retry is safe. */
    data object Offline : SessionState

    /** Biometric lock on and not yet unlocked in this process. */
    data object Locked : SessionState
    data object PasswordChangeRequired : SessionState
    data object Active : SessionState
}
