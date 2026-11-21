package vn.shelftap.app.ui.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.models.AppError
import vn.shelftap.app.data.repository.AuthRepository

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val success: Boolean = false
)

class ChangePasswordViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun onCurrentPasswordChanged(password: String) {
        _uiState.update { it.copy(currentPassword = password, errorMessage = null) }
    }

    fun onNewPasswordChanged(password: String) {
        _uiState.update { it.copy(newPassword = password, errorMessage = null) }
    }

    fun onConfirmNewPasswordChanged(password: String) {
        _uiState.update { it.copy(confirmNewPassword = password, errorMessage = null) }
    }

    fun changePassword() {
        val state = _uiState.value
        val current = state.currentPassword
        val new = state.newPassword
        val confirm = state.confirmNewPassword

        if (current.isBlank() || new.isBlank() || confirm.isBlank()) {
            _uiState.update { it.copy(errorMessage = "All fields are required.") }
            return
        }

        if (new != confirm) {
            _uiState.update { it.copy(errorMessage = "New passwords do not match.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = authRepository.changePassword(currentPassword = current, newPassword = new)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, success = true, errorMessage = null) }
                },
                onFailure = { throwable ->
                    val message = when (throwable) {
                        is AppError.InvalidCredentials -> throwable.message
                        is AppError.RateLimited -> throwable.message
                        is AppError.TokenInvalid -> throwable.message
                        is AppError.Network -> throwable.message
                        is AppError.Unknown -> throwable.message
                        else -> throwable.localizedMessage ?: "Failed to change password. Please try again."
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                }
            )
        }
    }
}
