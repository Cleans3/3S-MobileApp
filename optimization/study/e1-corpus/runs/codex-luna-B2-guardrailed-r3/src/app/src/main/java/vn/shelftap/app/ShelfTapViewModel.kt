package vn.shelftap.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ScreenState {
    data object Loading : ScreenState
    data class SignedOut(val message: String? = null) : ScreenState
    data class SignedIn(val member: Member, val changingPassword: Boolean = false) : ScreenState
    data class Error(val message: String) : ScreenState
}

class ShelfTapViewModel(app: Application) : AndroidViewModel(app) {
    private val storage = SessionStore(app)
    private val api = ShelfTapApi(storage, app)
    private val _state = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val state: StateFlow<ScreenState> = _state.asStateFlow()

    init { restore() }

    private fun restore() = viewModelScope.launch {
        val hasSession = withContext(Dispatchers.IO) { storage.read() != null }
        if (!hasSession) { _state.value = ScreenState.SignedOut(); return@launch }
        runCatching { api.member() }.onSuccess { member ->
            _state.value = ScreenState.SignedIn(member, member.mustChangePassword)
        }.onFailure { error ->
            if (error is ApiException && error.code == "TOKEN_INVALID") { withContext(Dispatchers.IO) { storage.clear() }; _state.value = ScreenState.SignedOut("Your session has expired. Please sign in again.") }
            else _state.value = ScreenState.Error(messageFor(error))
        }
    }

    fun signIn(email: String, password: String) = viewModelScope.launch {
        if (email.isBlank() || password.isBlank()) { _state.value = ScreenState.SignedOut("Please enter your email and password."); return@launch }
        _state.value = ScreenState.Loading
        runCatching {
            val session = api.login(email.trim(), password)
            withContext(Dispatchers.IO) { storage.write(session) }
            api.member()
        }.onSuccess { member -> _state.value = ScreenState.SignedIn(member, member.mustChangePassword) }
            .onFailure { _state.value = ScreenState.SignedOut(messageFor(it)) }
    }

    fun beginChangePassword() { (_state.value as? ScreenState.SignedIn)?.let { _state.value = it.copy(changingPassword = true) } }

    fun changePassword(current: String, next: String, confirm: String) = viewModelScope.launch {
        if (current.isBlank() || next.isBlank() || confirm.isBlank()) { _state.value = ScreenState.Error("Please complete all password fields."); return@launch }
        if (next != confirm) { _state.value = ScreenState.Error("New passwords do not match."); return@launch }
        val signedIn = _state.value as? ScreenState.SignedIn ?: return@launch
        _state.value = signedIn.copy(changingPassword = true)
        runCatching { api.changePassword(current, next); api.member() }
            .onSuccess { _state.value = ScreenState.SignedIn(it, false) }
            .onFailure { _state.value = ScreenState.Error(messageFor(it)) }
    }

    fun signOut() = viewModelScope.launch {
        withContext(Dispatchers.IO) { api.logout(); storage.clear() }
        _state.value = ScreenState.SignedOut()
    }

    fun dismissError() { if (_state.value is ScreenState.Error) _state.value = ScreenState.SignedOut() }

    private fun messageFor(error: Throwable): String = when (error) {
        is ApiException -> when (error.code) {
            "INVALID_CREDENTIALS" -> "Email or password is incorrect."
            "ACCOUNT_LOCKED" -> "Your account is locked. Contact the library."
            "RATE_LIMITED" -> "Too many attempts. Try again in ${error.retryAfterSeconds ?: 60} seconds."
            "PASSWORD_CHANGE_REQUIRED" -> "Please change your password to continue."
            "TOKEN_INVALID" -> "Your session has expired. Please sign in again."
            else -> "Something went wrong. Please try again."
        }
        else -> "Something went wrong. Please try again."
    }
}
