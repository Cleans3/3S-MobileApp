package vn.shelftap.app.ui.screens.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.remote.ApiException
import vn.shelftap.app.data.remote.ErrorCodes
import vn.shelftap.app.data.remote.toUserMessage
import vn.shelftap.app.data.repository.MemberRepository

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val isCurrentPasswordVisible: Boolean = false,
    val isNewPasswordVisible: Boolean = false,
    val isConfirmNewPasswordVisible: Boolean = false,
    val isMandatory: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

sealed interface ChangePasswordNavigationEvent {
    data object NavigateToProfile : ChangePasswordNavigationEvent
    data object NavigateToSignIn : ChangePasswordNavigationEvent
}

class ChangePasswordViewModel(
    private val memberRepository: MemberRepository,
    isMandatory: Boolean
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState(isMandatory = isMandatory))
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    private val _navEvents = MutableSharedFlow<ChangePasswordNavigationEvent>()
    val navEvents: SharedFlow<ChangePasswordNavigationEvent> = _navEvents.asSharedFlow()

    fun onCurrentPasswordChanged(value: String) {
        _uiState.update { it.copy(currentPassword = value, errorMessage = null) }
    }

    fun onNewPasswordChanged(value: String) {
        _uiState.update { it.copy(newPassword = value, errorMessage = null) }
    }

    fun onConfirmNewPasswordChanged(value: String) {
        _uiState.update { it.copy(confirmNewPassword = value, errorMessage = null) }
    }

    fun toggleCurrentPasswordVisibility() {
        _uiState.update { it.copy(isCurrentPasswordVisible = !it.isCurrentPasswordVisible) }
    }

    fun toggleNewPasswordVisibility() {
        _uiState.update { it.copy(isNewPasswordVisible = !it.isNewPasswordVisible) }
    }

    fun toggleConfirmNewPasswordVisibility() {
        _uiState.update { it.copy(isConfirmNewPasswordVisible = !it.isConfirmNewPasswordVisible) }
    }

    fun changePassword() {
        val currentPassword = _uiState.value.currentPassword
        val newPassword = _uiState.value.newPassword
        val confirmNewPassword = _uiState.value.confirmNewPassword

        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "All fields are required.") }
            return
        }

        // The two new entries must match
        if (newPassword != confirmNewPassword) {
            _uiState.update { it.copy(errorMessage = "The new passwords do not match.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                memberRepository.changePassword(
                    currentPassword = currentPassword,
                    newPassword = newPassword
                )
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                _navEvents.emit(ChangePasswordNavigationEvent.NavigateToProfile)
            } catch (e: ApiException) {
                val message = when (e.error.code) {
                    ErrorCodes.INVALID_CREDENTIALS -> {
                        e.error.detail ?: "Current password is incorrect. Please try again."
                    }
                    ErrorCodes.ACCOUNT_LOCKED -> {
                        e.error.detail ?: "Your account has been locked. Please contact library support."
                    }
                    ErrorCodes.RATE_LIMITED -> {
                        val seconds = e.error.retryAfterSeconds
                        if (seconds != null) {
                            "Too many requests. Please wait $seconds seconds before trying again."
                        } else {
                            e.error.detail ?: "Too many requests. Please try again later."
                        }
                    }
                    ErrorCodes.TOKEN_INVALID -> {
                        _navEvents.emit(ChangePasswordNavigationEvent.NavigateToSignIn)
                        e.error.detail ?: "Session expired. Please sign in again."
                    }
                    ErrorCodes.PASSWORD_CHANGE_REQUIRED -> {
                        e.error.detail ?: "Password change is required."
                    }
                    else -> e.error.toUserMessage("Failed to change password. Please try again.")
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = message) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Unable to connect to server. Please check your network connection."
                    )
                }
            }
        }
    }

    class Factory(
        private val memberRepository: MemberRepository,
        private val isMandatory: Boolean
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChangePasswordViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ChangePasswordViewModel(memberRepository, isMandatory) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
