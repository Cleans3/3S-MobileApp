package vn.shelftap.app.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.shelftap.app.data.ApiException
import vn.shelftap.app.data.AuthRepository
import vn.shelftap.app.data.Me
import vn.shelftap.app.data.NETWORK_MESSAGE
import vn.shelftap.app.data.SESSION_ENDED_NOTICE
import vn.shelftap.app.data.SessionEndedException
import vn.shelftap.app.data.userMessage

sealed interface AppUiState {
    /** Startup check or a request in progress. */
    data object Loading : AppUiState

    data class SignedOut(val notice: String? = null) : AppUiState

    /** A stored session exists but the member's details could not be loaded. */
    data class Unavailable(val message: String) : AppUiState

    data class SignedIn(val me: Me) : AppUiState

    /** [forced] is true when the member must change the password before anything else. */
    data class ChangePassword(val forced: Boolean) : AppUiState
}

/**
 * Owns the app-level state machine. Screens call into it; it survives rotation.
 * Form-level errors are returned through callbacks so each screen can show them.
 */
class AppViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow<AppUiState>(AppUiState.Loading)
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (auth.hasSession) {
                loadProfile()
            } else {
                _state.value = AppUiState.SignedOut()
            }
        }
    }

    /** [onComplete] receives null on success, or the message to show under the form. */
    fun signIn(email: String, password: String, onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            val error: String? = try {
                val tokens = auth.login(email, password)
                if (tokens.mustChangePassword) {
                    _state.value = AppUiState.ChangePassword(forced = true)
                } else {
                    loadProfile()
                }
                null
            } catch (e: ApiException) {
                e.userMessage()
            } catch (e: IOException) {
                NETWORK_MESSAGE
            }
            onComplete(error)
        }
    }

    fun retry() {
        viewModelScope.launch { loadProfile() }
    }

    fun openChangePassword() {
        _state.value = AppUiState.ChangePassword(forced = false)
    }

    fun closeChangePassword() {
        viewModelScope.launch { loadProfile() }
    }

    /** [onComplete] receives null on success, or the message to show under the form. */
    fun changePassword(currentPassword: String, newPassword: String, onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            val error: String? = try {
                auth.changePassword(currentPassword, newPassword)
                loadProfile()
                // The server may still say a change is required; say so instead of clearing silently.
                if (_state.value is AppUiState.ChangePassword) {
                    "The password change was not accepted. Please try again."
                } else {
                    null
                }
            } catch (e: SessionEndedException) {
                _state.value = AppUiState.SignedOut(SESSION_ENDED_NOTICE)
                null
            } catch (e: ApiException) {
                if (e.code == "INVALID_CREDENTIALS") {
                    "Your current password is not correct."
                } else {
                    e.userMessage()
                }
            } catch (e: IOException) {
                NETWORK_MESSAGE
            }
            onComplete(error)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _state.value = AppUiState.Loading
            auth.logout()
            _state.value = AppUiState.SignedOut()
        }
    }

    private suspend fun loadProfile() {
        _state.value = AppUiState.Loading
        _state.value = try {
            val me = auth.me()
            if (me.mustChangePassword) {
                AppUiState.ChangePassword(forced = true)
            } else {
                AppUiState.SignedIn(me)
            }
        } catch (e: SessionEndedException) {
            AppUiState.SignedOut(SESSION_ENDED_NOTICE)
        } catch (e: ApiException) {
            if (e.code == "PASSWORD_CHANGE_REQUIRED") {
                AppUiState.ChangePassword(forced = true)
            } else {
                AppUiState.Unavailable(e.userMessage())
            }
        } catch (e: IOException) {
            AppUiState.Unavailable(NETWORK_MESSAGE)
        }
    }
}
