package vn.shelftap.app.ui.password

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
import vn.shelftap.app.data.SessionEndedException
import vn.shelftap.app.ui.ErrorKind
import vn.shelftap.app.ui.UiError
import vn.shelftap.app.ui.toUiError

data class ChangePasswordUiState(
    val current: String = "",
    val newPassword: String = "",
    val confirm: String = "",
    val submitting: Boolean = false,
    val error: UiError? = null,
    val saved: Boolean = false,
)

/**
 * [required] is true when the member must change the password before anything else
 * (mustChangePassword). In that mode the screen cannot be left by back navigation.
 */
class ChangePasswordViewModel(
    private val accounts: AccountRepository,
    val required: Boolean,
) : ViewModel() {
    private val _state = MutableStateFlow(ChangePasswordUiState())
    val state: StateFlow<ChangePasswordUiState> = _state.asStateFlow()

    fun onCurrentChange(value: String) {
        _state.update { it.copy(current = value, error = null) }
    }

    fun onNewChange(value: String) {
        _state.update { it.copy(newPassword = value, error = null) }
    }

    fun onConfirmChange(value: String) {
        _state.update { it.copy(confirm = value, error = null) }
    }

    fun submit() {
        val form = _state.value
        if (form.submitting) return
        if (form.current.isEmpty() || form.newPassword.isEmpty() || form.confirm.isEmpty()) {
            _state.update { it.copy(error = UiError(ErrorKind.MISSING_FIELDS)) }
            return
        }
        if (form.newPassword != form.confirm) {
            _state.update { it.copy(error = UiError(ErrorKind.PASSWORDS_DO_NOT_MATCH)) }
            return
        }
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            try {
                accounts.changePassword(form.current, form.newPassword)
                _state.value = ChangePasswordUiState(saved = true)
            } catch (e: SessionEndedException) {
                // The root navigation has already moved the member to sign-in.
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(submitting = false, error = e.toUiError(forPasswordChange = true))
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            accounts.signOut()
        }
    }

    companion object {
        fun factory(accounts: AccountRepository, required: Boolean): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { ChangePasswordViewModel(accounts, required) }
            }
    }
}
