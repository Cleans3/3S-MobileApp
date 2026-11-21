package vn.shelftap.app.ui.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.AccountRepository
import vn.shelftap.app.ui.ErrorKind
import vn.shelftap.app.ui.UiError
import vn.shelftap.app.ui.toUiError

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val submitting: Boolean = false,
    val error: UiError? = null,
)

class SignInViewModel(private val accounts: AccountRepository) : ViewModel() {
    private val _state = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) {
        _state.update { it.copy(email = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, error = null) }
    }

    fun submit() {
        val form = _state.value
        if (form.submitting) return
        if (form.email.isBlank() || form.password.isEmpty()) {
            _state.update { it.copy(error = UiError(ErrorKind.MISSING_FIELDS)) }
            return
        }
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            try {
                accounts.signIn(form.email.trim(), form.password)
                // On success the session status changes and the root navigation takes over.
                _state.update { it.copy(submitting = false, password = "") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(submitting = false, error = e.toUiError()) }
            }
        }
    }

    companion object {
        fun factory(accounts: AccountRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { SignInViewModel(accounts) }
        }
    }
}
