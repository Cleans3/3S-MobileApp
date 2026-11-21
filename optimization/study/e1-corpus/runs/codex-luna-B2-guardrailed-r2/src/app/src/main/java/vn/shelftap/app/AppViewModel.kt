package vn.shelftap.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AppState {
    data object Loading : AppState
    data object SignedOut : AppState
    data class Profile(val member: Member) : AppState
    data class PasswordChange(val member: Member) : AppState
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SessionRepository(application)
    private val _state = MutableStateFlow<AppState>(AppState.Loading)
    val state: StateFlow<AppState> = _state.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = repository.restore()?.let { destination(it) } ?: AppState.SignedOut
        }
    }

    fun signIn(email: String, password: String) = launchRequest {
        _state.value = destination(repository.signIn(email, password))
    }

    fun savePassword(current: String, new: String) = launchRequest {
        _state.value = AppState.Profile(repository.changePassword(current, new))
    }

    fun showPasswordChange() {
        val current = (_state.value as? AppState.Profile)?.member ?: return
        _error.value = null
        _state.value = AppState.PasswordChange(current)
    }

    fun signOut() = launchRequest {
        repository.signOut()
        _state.value = AppState.SignedOut
    }

    private fun destination(member: Member): AppState =
        if (member.mustChangePassword) AppState.PasswordChange(member) else AppState.Profile(member)

    private fun launchRequest(action: suspend () -> Unit) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try { action() } catch (failure: ApiFailure) {
                if (failure.code == "TOKEN_INVALID") _state.value = AppState.SignedOut
                _error.value = messageFor(failure)
            } catch (_: Exception) {
                _error.value = "We couldn't connect right now. Please try again."
            } finally { _busy.value = false }
        }
    }

    private fun messageFor(error: ApiFailure): String = when (error.code) {
        "INVALID_CREDENTIALS" -> "The email or password is incorrect."
        "ACCOUNT_LOCKED" -> "This account is locked. Please contact the library."
        "RATE_LIMITED" -> "Too many attempts. Try again in ${error.retryAfterSeconds ?: 60} seconds."
        "PASSWORD_CHANGE_REQUIRED" -> "You must change your password before continuing."
        "TOKEN_INVALID" -> "Your session has expired. Please sign in again."
        else -> error.detail ?: "Something went wrong. Please try again."
    }
}
