package vn.shelftap.app.ui.signin

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

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class SignInViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(
            email = email,
            errorMessage = null
        )
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            errorMessage = null
        )
    }

    fun signIn() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password

        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter your email address.")
            return
        }

        if (password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter your password.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = authRepository.login(email, password)
            if (result.isFailure) {
                val error = result.exceptionOrNull()
                val message = mapErrorMessage(error)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = message
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
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
                    detail ?: "Invalid email or password. Please try again."
                }
                ApiErrorCodes.ACCOUNT_LOCKED -> {
                    detail ?: "Your account has been locked. Please contact library staff."
                }
                ApiErrorCodes.RATE_LIMITED -> {
                    if (retryAfter != null) {
                        "Too many login attempts. Please try again in $retryAfter seconds."
                    } else {
                        detail ?: "Too many attempts. Please try again shortly."
                    }
                }
                ApiErrorCodes.TOKEN_INVALID -> {
                    detail ?: "Session could not be established. Please try again."
                }
                ApiErrorCodes.PASSWORD_CHANGE_REQUIRED -> {
                    detail ?: "Password change is required."
                }
                else -> {
                    detail ?: error.error.title ?: "Authentication failed (${error.httpCode})."
                }
            }
        }
        return error?.localizedMessage ?: "Unable to connect to the server. Please check your connection."
    }

    companion object {
        fun provideFactory(authRepository: AuthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SignInViewModel(authRepository) as T
                }
            }
    }
}
