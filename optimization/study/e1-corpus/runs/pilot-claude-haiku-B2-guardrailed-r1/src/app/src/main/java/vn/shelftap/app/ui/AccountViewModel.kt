package vn.shelftap.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONException
import vn.shelftap.app.data.AccountRepository
import vn.shelftap.app.data.ApiException
import vn.shelftap.app.data.Member
import vn.shelftap.app.data.SessionEndedException
import java.io.IOException

sealed interface Screen {
    data object Loading : Screen
    data object SignIn : Screen
    data object Profile : Screen
    /** [forced] is true when the member must change the password before anything else. */
    data class ChangePassword(val forced: Boolean) : Screen
}

/** Error messages as data; the UI maps them to string resources. */
sealed interface UiError {
    data object MissingFields : UiError
    data object InvalidCredentials : UiError
    data object AccountLocked : UiError
    data class RateLimited(val seconds: Int) : UiError
    data object SessionEnded : UiError
    data object CurrentPasswordWrong : UiError
    data object PasswordMismatch : UiError
    data object Network : UiError
    data object Generic : UiError
}

data class AccountUiState(
    val screen: Screen = Screen.Loading,
    val busy: Boolean = false,
    val signInError: UiError? = null,
    val profile: Member? = null,
    val profileError: UiError? = null,
    val passwordError: UiError? = null,
)

class AccountViewModel(private val repo: AccountRepository) : ViewModel() {

    private val _state = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (repo.hasSession()) openSession() else showSignIn(null)
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isEmpty()) {
            _state.update { it.copy(signInError = UiError.MissingFields) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, signInError = null) }
            try {
                val mustChange = repo.signIn(email.trim(), password)
                if (mustChange) showForcedChange() else openSession()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.update { it.copy(busy = false, signInError = e.toUiError()) }
            }
        }
    }

    fun retryProfile() {
        viewModelScope.launch { openSession() }
    }

    fun signOut() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            repo.signOut()
            showSignIn(null)
        }
    }

    fun openChangePassword() {
        _state.update { it.copy(screen = Screen.ChangePassword(forced = false), passwordError = null) }
    }

    fun closeChangePassword() {
        _state.update { it.copy(screen = Screen.Profile, passwordError = null) }
    }

    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        if (currentPassword.isEmpty() || newPassword.isEmpty()) {
            _state.update { it.copy(passwordError = UiError.MissingFields) }
            return
        }
        if (newPassword != confirmPassword) {
            _state.update { it.copy(passwordError = UiError.PasswordMismatch) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, passwordError = null) }
            try {
                repo.changePassword(currentPassword, newPassword)
                // Reloads the profile and leaves the change-password screen.
                openSession()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                when {
                    e is SessionEndedException -> showSignIn(UiError.SessionEnded)
                    e is ApiException && e.code == "INVALID_CREDENTIALS" ->
                        _state.update { it.copy(busy = false, passwordError = UiError.CurrentPasswordWrong) }
                    else ->
                        _state.update { it.copy(busy = false, passwordError = e.toUiError()) }
                }
            }
        }
    }

    /** Loads /api/me and opens the profile, or forces a password change if the server requires one. */
    private suspend fun openSession() {
        _state.update { it.copy(busy = true, profileError = null) }
        try {
            val me = repo.currentMember()
            if (me.mustChangePassword) {
                showForcedChange()
            } else {
                _state.update {
                    it.copy(
                        busy = false,
                        screen = Screen.Profile,
                        profile = me,
                        profileError = null,
                        signInError = null,
                    )
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            when {
                e is SessionEndedException -> showSignIn(UiError.SessionEnded)
                e is ApiException && e.code == "PASSWORD_CHANGE_REQUIRED" -> showForcedChange()
                else -> _state.update {
                    it.copy(busy = false, screen = Screen.Profile, profileError = e.toUiError())
                }
            }
        }
    }

    private fun showSignIn(error: UiError?) {
        _state.update { AccountUiState(screen = Screen.SignIn, signInError = error) }
    }

    private fun showForcedChange() {
        _state.update {
            it.copy(
                busy = false,
                screen = Screen.ChangePassword(forced = true),
                passwordError = null,
                signInError = null,
            )
        }
    }

    private fun Exception.toUiError(): UiError = when (this) {
        is SessionEndedException -> UiError.SessionEnded
        is ApiException -> when (code) {
            "INVALID_CREDENTIALS" -> UiError.InvalidCredentials
            "ACCOUNT_LOCKED" -> UiError.AccountLocked
            "RATE_LIMITED" -> UiError.RateLimited(retryAfterSeconds ?: DEFAULT_RETRY_SECONDS)
            "TOKEN_INVALID" -> UiError.SessionEnded
            else -> UiError.Generic
        }
        is IOException -> UiError.Network
        is JSONException -> UiError.Generic
        else -> UiError.Generic
    }

    private companion object {
        const val DEFAULT_RETRY_SECONDS = 60
    }
}

class AccountViewModelFactory(private val repo: AccountRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AccountViewModel(repo) as T
}
