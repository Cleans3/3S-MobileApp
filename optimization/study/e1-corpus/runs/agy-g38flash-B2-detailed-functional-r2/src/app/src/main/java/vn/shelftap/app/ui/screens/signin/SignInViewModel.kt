package vn.shelftap.app.ui.screens.signin

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
import vn.shelftap.app.data.repository.AuthRepository

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface SignInNavigationEvent {
    data object NavigateToProfile : SignInNavigationEvent
    data object NavigateToChangePassword : SignInNavigationEvent
}

class SignInViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    private val _navEvents = MutableSharedFlow<SignInNavigationEvent>()
    val navEvents: SharedFlow<SignInNavigationEvent> = _navEvents.asSharedFlow()

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun signIn() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password

        if (email.isEmpty() || password.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please enter both email and password.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val authResponse = authRepository.login(email, password)
                _uiState.update { it.copy(isLoading = false) }

                if (authResponse.mustChangePassword) {
                    _navEvents.emit(SignInNavigationEvent.NavigateToChangePassword)
                } else {
                    _navEvents.emit(SignInNavigationEvent.NavigateToProfile)
                }
            } catch (e: ApiException) {
                val friendlyMessage = when (e.error.code) {
                    ErrorCodes.INVALID_CREDENTIALS -> {
                        e.error.detail ?: "Invalid email or password. Please try again."
                    }
                    ErrorCodes.ACCOUNT_LOCKED -> {
                        e.error.detail ?: "Your account has been locked. Please contact library support."
                    }
                    ErrorCodes.RATE_LIMITED -> {
                        val seconds = e.error.retryAfterSeconds
                        if (seconds != null) {
                            "Too many sign-in attempts. Please wait $seconds seconds before trying again."
                        } else {
                            e.error.detail ?: "Too many sign-in attempts. Please try again later."
                        }
                    }
                    ErrorCodes.PASSWORD_CHANGE_REQUIRED -> {
                        e.error.detail ?: "Password change is required before signing in."
                    }
                    ErrorCodes.TOKEN_INVALID -> {
                        e.error.detail ?: "Session invalid. Please sign in again."
                    }
                    else -> {
                        e.error.toUserMessage("Sign-in failed. Please check your credentials.")
                    }
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = friendlyMessage
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Unable to connect to the library server. Please check your network connection."
                    )
                }
            }
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SignInViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SignInViewModel(authRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
