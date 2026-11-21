package vn.shelftap.app.ui.screens.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.model.ApiResult
import vn.shelftap.app.data.repository.ShelfTapRepository

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ChangePasswordViewModel(
    private val repository: ShelfTapRepository
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

    fun submitChangePassword(
        onSuccess: () -> Unit,
        onSessionExpired: () -> Unit
    ) {
        val currentPassword = _uiState.value.currentPassword
        val newPassword = _uiState.value.newPassword
        val confirmNewPassword = _uiState.value.confirmNewPassword

        if (currentPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Current password cannot be empty.") }
            return
        }
        if (newPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "New password cannot be empty.") }
            return
        }
        if (newPassword != confirmNewPassword) {
            _uiState.update { it.copy(errorMessage = "The two new passwords do not match.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = repository.changePassword(currentPassword, newPassword)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                    onSuccess()
                }
                is ApiResult.Error -> {
                    when (result.code) {
                        ShelfTapRepository.CODE_TOKEN_INVALID -> {
                            _uiState.update { it.copy(isLoading = false) }
                            onSessionExpired()
                        }
                        ShelfTapRepository.CODE_RATE_LIMITED -> {
                            val retry = result.retryAfterSeconds ?: 60
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "Too many requests. Please retry after $retry seconds."
                                )
                            }
                        }
                        ShelfTapRepository.CODE_INVALID_CREDENTIALS -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "Current password is incorrect."
                                )
                            }
                        }
                        else -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = result.message ?: "Failed to change password (${result.statusCode})."
                                )
                            }
                        }
                    }
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
