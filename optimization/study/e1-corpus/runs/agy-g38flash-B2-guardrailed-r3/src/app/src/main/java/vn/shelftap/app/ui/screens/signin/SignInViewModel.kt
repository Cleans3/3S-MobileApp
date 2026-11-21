package vn.shelftap.app.ui.screens.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.model.ApiResult
import vn.shelftap.app.data.repository.ShelfTapRepository

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class SignInViewModel(
    private val repository: ShelfTapRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun onEmailChanged(newEmail: String) {
        _uiState.update { it.copy(email = newEmail, errorMessage = null) }
    }

    fun onPasswordChanged(newPassword: String) {
        _uiState.update { it.copy(password = newPassword, errorMessage = null) }
    }

    fun signIn(
        onSuccess: (mustChangePassword: Boolean) -> Unit
    ) {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter both email and password.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = repository.login(email = email, password = password)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                    onSuccess(result.data.mustChangePassword)
                }
                is ApiResult.Error -> {
                    val message = when (result.code) {
                        ShelfTapRepository.CODE_INVALID_CREDENTIALS -> "Invalid email or password."
                        ShelfTapRepository.CODE_ACCOUNT_LOCKED -> "Your account has been locked. Please contact library support."
                        ShelfTapRepository.CODE_RATE_LIMITED -> {
                            val retry = result.retryAfterSeconds ?: 60
                            "Too many requests. Please retry after $retry seconds."
                        }
                        ShelfTapRepository.CODE_PASSWORD_CHANGE_REQUIRED -> {
                            onSuccess(true)
                            return@launch
                        }
                        else -> result.message ?: "An error occurred (${result.statusCode}). Please try again."
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                }
                is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Unable to connect to the server. Please check your network connection."
                        )
                    }
                }
            }
        }
    }
}
