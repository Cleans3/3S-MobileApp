package vn.shelftap.app.ui.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.shelftap.app.data.model.ApiErrorCodes
import vn.shelftap.app.data.model.ApiException
import vn.shelftap.app.data.repository.AuthRepository

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class ChangePasswordViewModel(
    private val authRepository: AuthRepository,
    val isMandatory: Boolean
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun onCurrentPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(
            currentPassword = password,
            errorMessage = null
        )
    }

    fun onNewPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(
            newPassword = password,
            errorMessage = null
        )
    }

    fun onConfirmNewPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(
            confirmNewPassword = password,
            errorMessage = null
        )
    }

    fun submitChangePassword() {
        val current = _uiState.value.currentPassword
        val newPw = _uiState.value.newPassword
        val confirmPw = _uiState.value.confirmNewPassword

        if (current.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter your current password.")
            return
        }

        if (newPw.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a new password.")
            return
        }

        // The two new entries must match
        if (newPw != confirmPw) {
            _uiState.value = _uiState.value.copy(errorMessage = "The new passwords do not match.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = authRepository.changePassword(
                currentPassword = current,
                newPassword = newPw
            )

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true,
                    errorMessage = null
                )
            } else {
                val error = result.exceptionOrNull()
                val message = mapErrorMessage(error)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = message
                )
            }
        }
    }

    private fun mapErrorMessage(error: Throwable?): String {
        if (error is ApiException) {
            val code = error.error.code
            val detail = error.error.detail
            val retryAfter = error.retryAfterSeconds

            return when (code) {
                ApiErrorCodes.INVALID_CREDENTIALS -> {
                    detail ?: "Current password is incorrect."
                }
                ApiErrorCodes.ACCOUNT_LOCKED -> {
                    detail ?: "Account is locked. Please contact the library."
                }
                ApiErrorCodes.RATE_LIMITED -> {
                    if (retryAfter != null) {
                        "Too many requests. Please wait $retryAfter seconds before trying again."
                    } else {
                        detail ?: "Too many attempts. Please try again shortly."
                    }
                }
                ApiErrorCodes.PASSWORD_CHANGE_REQUIRED -> {
                    detail ?: "Password change is required."
                }
                else -> {
                    detail ?: error.error.title ?: "Password change failed (${error.httpCode})."
                }
            }
        }
        return error?.localizedMessage ?: "Failed to update password. Please check your connection."
    }

    companion object {
        fun provideFactory(authRepository: AuthRepository, isMandatory: Boolean): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChangePasswordViewModel(authRepository, isMandatory) as T
                }
            }
    }
}
