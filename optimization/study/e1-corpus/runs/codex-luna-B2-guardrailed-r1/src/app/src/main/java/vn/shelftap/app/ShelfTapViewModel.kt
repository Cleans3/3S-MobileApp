package vn.shelftap.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScreenState {
    data object Loading : ScreenState
    data class SignIn(val message: String? = null, val busy: Boolean = false) : ScreenState
    data class ChangePassword(val message: String? = null, val busy: Boolean = false) : ScreenState
    data class Profile(val member: Member, val busy: Boolean = false) : ScreenState
}

class ShelfTapViewModel(private val repository: ShelfTapRepository) : ViewModel() {
    private val _state = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val state: StateFlow<ScreenState> = _state.asStateFlow()

    init { restoreSession() }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = ScreenState.SignIn("Please complete all fields.")
            return
        }
        _state.value = ScreenState.SignIn(busy = true)
        viewModelScope.launch {
            try {
                if (repository.signIn(email.trim(), password)) {
                    _state.value = ScreenState.ChangePassword()
                } else {
                    loadMember()
                }
            } catch (error: Throwable) {
                _state.value = if (error is ApiException && error.code == "PASSWORD_CHANGE_REQUIRED") {
                    ScreenState.ChangePassword()
                } else {
                    ScreenState.SignIn(messageFor(error))
                }
            }
        }
    }

    fun savePassword(current: String, new: String, confirm: String) {
        if (current.isBlank() || new.isBlank() || confirm.isBlank()) {
            _state.value = ScreenState.ChangePassword("Please complete all fields.")
            return
        }
        if (new != confirm) {
            _state.value = ScreenState.ChangePassword("New passwords do not match.")
            return
        }
        _state.value = ScreenState.ChangePassword(busy = true)
        viewModelScope.launch {
            try {
                repository.changePassword(current, new)
                loadMember()
            } catch (error: Throwable) {
                _state.value = ScreenState.ChangePassword(messageFor(error))
            }
        }
    }

    fun signOut() {
        val current = _state.value
        if (current !is ScreenState.Profile) return
        _state.value = current.copy(busy = true)
        viewModelScope.launch {
            runCatching { repository.signOut() }
            _state.value = ScreenState.SignIn()
        }
    }

    private fun restoreSession() = viewModelScope.launch {
        try {
            if (repository.hasPendingPasswordChange()) {
                _state.value = ScreenState.ChangePassword()
            } else {
                loadMember()
            }
        } catch (error: Throwable) {
            _state.value = if (error is ApiException && error.code == "PASSWORD_CHANGE_REQUIRED") {
                ScreenState.ChangePassword()
            } else {
                ScreenState.SignIn(messageFor(error))
            }
        }
    }

    private suspend fun loadMember() {
        val member = repository.currentMember()
        _state.value = when {
            member == null -> ScreenState.SignIn()
            member.mustChangePassword -> ScreenState.ChangePassword()
            else -> ScreenState.Profile(member)
        }
    }

    private fun messageFor(error: Throwable): String = when (error) {
        is ApiException -> when (error.code) {
            "INVALID_CREDENTIALS" -> "Email or password is incorrect."
            "ACCOUNT_LOCKED" -> "Your account is locked. Please contact the library."
            "RATE_LIMITED" -> "Too many attempts. Try again in ${error.retryAfterSeconds ?: 60} seconds."
            "PASSWORD_CHANGE_REQUIRED" -> "Please change your password to continue."
            else -> "Something went wrong. Please try again."
        }
        else -> "Something went wrong. Please try again."
    }
}
