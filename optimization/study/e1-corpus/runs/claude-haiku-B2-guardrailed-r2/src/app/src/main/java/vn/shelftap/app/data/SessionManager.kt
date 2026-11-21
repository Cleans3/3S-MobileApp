package vn.shelftap.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SessionStatus {
    /** The stored session has not been read yet. */
    data object Unknown : SessionStatus
    data object SignedOut : SessionStatus
    data class SignedIn(val mustChangePassword: Boolean) : SessionStatus
}

/** Single source of truth for whether a member is signed in. The UI navigates from [status]. */
class SessionManager(private val store: SessionStore) {
    private val _status = MutableStateFlow<SessionStatus>(SessionStatus.Unknown)
    val status: StateFlow<SessionStatus> = _status.asStateFlow()

    suspend fun restore() {
        store.deviceId() // creates the install's device id on first launch
        val session = store.loadSession()
        _status.value = if (session == null) {
            SessionStatus.SignedOut
        } else {
            SessionStatus.SignedIn(session.mustChangePassword)
        }
    }

    suspend fun currentSession(): StoredSession? = store.loadSession()

    suspend fun save(session: StoredSession) {
        store.saveSession(session)
        _status.value = SessionStatus.SignedIn(session.mustChangePassword)
    }

    suspend fun clear() {
        store.clearSession()
        _status.value = SessionStatus.SignedOut
    }

    suspend fun markPasswordChanged() = updateMustChangePassword(false)

    suspend fun markPasswordChangeRequired() = updateMustChangePassword(true)

    private suspend fun updateMustChangePassword(value: Boolean) {
        val session = store.loadSession() ?: return
        save(session.copy(mustChangePassword = value))
    }
}
