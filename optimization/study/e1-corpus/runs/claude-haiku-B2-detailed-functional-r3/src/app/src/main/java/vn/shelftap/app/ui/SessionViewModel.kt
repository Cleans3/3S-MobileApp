package vn.shelftap.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.shelftap.app.data.ApiException
import vn.shelftap.app.data.MeResponse
import vn.shelftap.app.data.SessionEndedException
import vn.shelftap.app.data.SessionRepository
import vn.shelftap.app.data.userMessage
import java.io.IOException

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    /** Signed in, but the member must change the password before anything else. */
    data object PasswordChangeRequired : SessionState
    data class SignedIn(val member: MeResponse) : SessionState
    /** A stored session exists but the profile could not be loaded (for example, offline). */
    data class Unavailable(val message: String) : SessionState
}

data class FormState(
    val busy: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
)

/** App-wide session state plus the state of the sign-in and change-password forms. */
class SessionViewModel(private val repo: SessionRepository) : ViewModel() {

    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _signInForm = MutableStateFlow(FormState())
    val signInForm: StateFlow<FormState> = _signInForm.asStateFlow()

    private val _passwordForm = MutableStateFlow(FormState())
    val passwordForm: StateFlow<FormState> = _passwordForm.asStateFlow()

    init {
        restore()
    }

    fun restore() {
        viewModelScope.launch {
            _state.value = SessionState.Loading
            _state.value = if (repo.initialize()) loadProfile() else SessionState.SignedOut
        }
    }

    fun signIn(email: String, password: String) {
        if (_signInForm.value.busy) return
        viewModelScope.launch {
            _signInForm.value = FormState(busy = true)
            try {
                val tokens = repo.signIn(email.trim(), password)
                _signInForm.value = FormState()
                if (tokens.mustChangePassword) {
                    _state.value = SessionState.PasswordChangeRequired
                } else {
                    _state.value = loadProfile()
                }
            } catch (e: Exception) {
                _signInForm.value = FormState(error = e.userMessage())
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repo.signOut()
            _passwordForm.value = FormState()
            _state.value = SessionState.SignedOut
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        if (_passwordForm.value.busy) return
        viewModelScope.launch {
            _passwordForm.value = FormState(busy = true)
            try {
                repo.changePassword(currentPassword, newPassword)
                // Reload the profile: this clears mustChangePassword and moves a forced change on to Profile.
                _state.value = loadProfile()
                _passwordForm.value = FormState(done = true)
            } catch (e: Exception) {
                _passwordForm.value = FormState(error = e.userMessage())
                applyGlobalFailure(e)
            }
        }
    }

    /** Clears the change-password form; called when that screen leaves the composition. */
    fun resetPasswordForm() {
        _passwordForm.value = FormState()
    }

    private suspend fun loadProfile(): SessionState =
        try {
            val me = repo.me()
            if (me.mustChangePassword) SessionState.PasswordChangeRequired else SessionState.SignedIn(me)
        } catch (e: SessionEndedException) {
            SessionState.SignedOut
        } catch (e: ApiException) {
            if (e.isPasswordChangeRequired) {
                SessionState.PasswordChangeRequired
            } else {
                SessionState.Unavailable(e.userMessage())
            }
        } catch (e: IOException) {
            SessionState.Unavailable(e.userMessage())
        }

    private fun applyGlobalFailure(e: Exception) {
        when {
            e is SessionEndedException -> _state.value = SessionState.SignedOut
            e is ApiException && e.isPasswordChangeRequired -> _state.value = SessionState.PasswordChangeRequired
        }
    }
}
