package vn.shelftap.app.ui.screens.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.model.ErrorResponse
import vn.shelftap.app.data.repository.ApiResult
import vn.shelftap.app.data.repository.AuthRepository

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val rateLimitCountdown: Int? = null,
    val isForced: Boolean = false
)

sealed class ChangePasswordNavigationEvent {
    object NavigateToProfile : ChangePasswordNavigationEvent()
    object NavigateToSignIn : ChangePasswordNavigationEvent()
}

class ChangePasswordViewModel(
    private val authRepository: AuthRepository,
    forced: Boolean
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState(isForced = forced))
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<ChangePasswordNavigationEvent>()
    val navigationEvent: SharedFlow<ChangePasswordNavigationEvent> = _navigationEvent.asSharedFlow()

    private var countdownJob: Job? = null

    fun onCurrentPasswordChanged(value: String) {
        _uiState.update { it.copy(currentPassword = value, errorMessage = null) }
    }

    fun onNewPasswordChanged(value: String) {
        _uiState.update { it.copy(newPassword = value, errorMessage = null) }
    }

    fun onConfirmNewPasswordChanged(value: String) {
        _uiState.update { it.copy(confirmNewPassword = value, errorMessage = null) }
    }

    fun submitChangePassword() {
        val current = _uiState.value.currentPassword
        val new = _uiState.value.newPassword
        val confirm = _uiState.value.confirmNewPassword

        if (current.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your current password.") }
            return
        }
        if (new.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a new password.") }
            return
        }
        // Requirement: the two new entries must match
        if (new != confirm) {
            _uiState.update { it.copy(errorMessage = "The new password and confirmation do not match.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.changePassword(current, new)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    _navigationEvent.emit(ChangePasswordNavigationEvent.NavigateToProfile)
                }
                is ApiResult.Error -> {
                    val error = result.error
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message,
                            rateLimitCountdown = error.retryAfterSeconds
                        )
                    }

                    if (error.code == ErrorResponse.CODE_RATE_LIMITED && error.retryAfterSeconds != null) {
                        startCountdown(error.retryAfterSeconds)
                    } else if (error.code == ErrorResponse.CODE_TOKEN_INVALID) {
                        _navigationEvent.emit(ChangePasswordNavigationEvent.NavigateToSignIn)
                    }
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.logout()
            _navigationEvent.emit(ChangePasswordNavigationEvent.NavigateToSignIn)
        }
    }

    private fun startCountdown(seconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (remaining in seconds downTo 1) {
                _uiState.update {
                    it.copy(
                        rateLimitCountdown = remaining,
                        errorMessage = "Rate limit reached. Please wait $remaining seconds before trying again."
                    )
                }
                delay(1000)
            }
            _uiState.update { it.copy(rateLimitCountdown = null, errorMessage = null) }
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val forced: Boolean
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChangePasswordViewModel(authRepository, forced) as T
        }
    }
}
