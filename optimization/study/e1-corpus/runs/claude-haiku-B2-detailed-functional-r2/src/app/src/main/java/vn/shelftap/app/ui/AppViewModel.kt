package vn.shelftap.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.ApiException
import vn.shelftap.app.data.AuthRepository
import vn.shelftap.app.data.MemberDto
import vn.shelftap.app.data.SessionExpiredException
import vn.shelftap.app.data.toUserMessage

enum class Destination { LOADING, SIGN_IN, PROFILE, CHANGE_PASSWORD }

data class AppState(
    val destination: Destination = Destination.LOADING,
    val member: MemberDto? = null,
    /** True when the member must change the password before anything else. Back navigation is blocked. */
    val forcedPasswordChange: Boolean = false,
    val busy: Boolean = false,
    val signInError: String? = null,
    val profileError: String? = null,
    val profileNotice: String? = null,
    val changePasswordError: String? = null,
)

class AppViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        // A stored session keeps the member signed in across app restarts.
        launchBusy {
            if (auth.hasSession()) loadMember() else showSignIn()
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isEmpty()) {
            _state.update { it.copy(signInError = "Enter your email and password.") }
            return
        }
        launchBusy {
            _state.update { it.copy(signInError = null) }
            val login = auth.signIn(email.trim(), password)
            if (login.mustChangePassword) showChangePassword(forced = true) else loadMember()
        }
    }

    fun signOut() = launchBusy {
        auth.signOut()
        showSignIn()
    }

    fun openChangePassword() {
        _state.update {
            it.copy(
                destination = Destination.CHANGE_PASSWORD,
                forcedPasswordChange = false,
                changePasswordError = null,
                profileNotice = null,
            )
        }
    }

    fun cancelChangePassword() {
        if (_state.value.forcedPasswordChange) return
        _state.update { it.copy(destination = Destination.PROFILE, changePasswordError = null) }
    }

    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            _state.update { it.copy(changePasswordError = "Fill in all fields.") }
            return
        }
        if (newPassword != confirmPassword) {
            _state.update { it.copy(changePasswordError = "The new passwords do not match.") }
            return
        }
        launchBusy {
            _state.update { it.copy(changePasswordError = null) }
            auth.changePassword(currentPassword, newPassword)
            loadMember()
            _state.update {
                if (it.destination == Destination.PROFILE) it.copy(profileNotice = "Password changed.") else it
            }
        }
    }

    /** Fetches /api/me and routes to the right screen. */
    private suspend fun loadMember() {
        val me = auth.me()
        if (me.mustChangePassword) {
            _state.update {
                it.copy(
                    member = me,
                    destination = Destination.CHANGE_PASSWORD,
                    forcedPasswordChange = true,
                    changePasswordError = null,
                )
            }
        } else {
            _state.update {
                it.copy(
                    member = me,
                    destination = Destination.PROFILE,
                    forcedPasswordChange = false,
                    profileError = null,
                    signInError = null,
                )
            }
        }
    }

    private fun showSignIn(message: String? = null) {
        _state.update { AppState(destination = Destination.SIGN_IN, signInError = message) }
    }

    private fun showChangePassword(forced: Boolean) {
        _state.update {
            it.copy(
                destination = Destination.CHANGE_PASSWORD,
                forcedPasswordChange = forced,
                changePasswordError = null,
            )
        }
    }

    private fun showError(message: String) {
        _state.update {
            when (it.destination) {
                Destination.LOADING -> it.copy(destination = Destination.SIGN_IN, signInError = message)
                Destination.SIGN_IN -> it.copy(signInError = message)
                Destination.PROFILE -> it.copy(profileError = message)
                Destination.CHANGE_PASSWORD -> it.copy(changePasswordError = message)
            }
        }
    }

    /** Runs [block] with a busy flag set, and routes any failure to the right place. */
    private fun launchBusy(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: SessionExpiredException) {
                showSignIn(e.toUserMessage())
            } catch (e: ApiException) {
                if (e.code == "PASSWORD_CHANGE_REQUIRED") {
                    showChangePassword(forced = true)
                } else {
                    showError(e.toUserMessage())
                }
            } catch (e: Exception) {
                showError(e.toUserMessage())
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }
}
