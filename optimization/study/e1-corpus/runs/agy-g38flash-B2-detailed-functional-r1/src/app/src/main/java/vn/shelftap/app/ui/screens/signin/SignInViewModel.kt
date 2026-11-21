package vn.shelftap.app.ui.screens.signin

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

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val rateLimitCountdown: Int? = null
)

sealed class SignInNavigationEvent {
    object NavigateToProfile : SignInNavigationEvent()
    object NavigateToChangePassword : SignInNavigationEvent()
}

class SignInViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<SignInNavigationEvent>()
    val navigationEvent: SharedFlow<SignInNavigationEvent> = _navigationEvent.asSharedFlow()

    private var countdownJob: Job? = null

    fun onEmailChanged(newEmail: String) {
        _uiState.update { it.copy(email = newEmail, errorMessage = null) }
    }

    fun onPasswordChanged(newPassword: String) {
        _uiState.update { it.copy(password = newPassword, errorMessage = null) }
    }

    fun signIn() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password

        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter both email and password.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.login(email, password)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    val response = result.data
                    if (response.mustChangePassword) {
                        _navigationEvent.emit(SignInNavigationEvent.NavigateToChangePassword)
                    } else {
                        _navigationEvent.emit(SignInNavigationEvent.NavigateToProfile)
                    }
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
                    }
                }
            }
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

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SignInViewModel(authRepository) as T
        }
    }
}
