package vn.shelftap.app.ui.changepassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.session.AuthState
import vn.shelftap.app.data.session.SessionRepository
import vn.shelftap.app.ui.UiError
import vn.shelftap.app.ui.toUiError

internal class ChangePasswordViewModel(private val session: SessionRepository) : ViewModel() {

    data class UiState(
        val isSubmitting: Boolean = false,
        val error: UiError? = null,
        /** True after a voluntary change, when the screen should close. */
        val leaveScreen: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** True while the member must change the password before using the app. */
    val isForced: StateFlow<Boolean> = session.authState
        .map { (it as? AuthState.SignedIn)?.mustChangePassword == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun submit(currentPassword: String, newPassword: String, confirmPassword: String) {
        if (_state.value.isSubmitting || _state.value.leaveScreen) return

        val validation = when {
            currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty() ->
                UiError.FieldsRequired
            newPassword != confirmPassword -> UiError.PasswordsDoNotMatch
            else -> null
        }
        if (validation != null) {
            _state.update { it.copy(error = validation) }
            return
        }

        _state.update { it.copy(isSubmitting = true, error = null) }
        val wasForced = isForced.value
        viewModelScope.launch {
            try {
                session.changePassword(currentPassword, newPassword)
                // A forced change ends with the root navigating to the profile; a voluntary one closes this screen.
                _state.update { it.copy(isSubmitting = false, leaveScreen = !wasForced) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(isSubmitting = false, error = e.toUiError(duringPasswordChange = true))
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            session.signOut()
        }
    }

    companion object {
        fun factory(session: SessionRepository) = viewModelFactory {
            initializer { ChangePasswordViewModel(session) }
        }
    }
}
