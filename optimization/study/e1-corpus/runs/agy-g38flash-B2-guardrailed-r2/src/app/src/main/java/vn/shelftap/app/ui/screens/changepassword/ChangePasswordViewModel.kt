package vn.shelftap.app.ui.screens.changepassword

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.shelftap.app.R
import vn.shelftap.app.data.repository.SessionRepository
import vn.shelftap.app.ui.common.ErrorFormatter

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val isCurrentPasswordVisible: Boolean = false,
    val isNewPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ChangePasswordViewModel(
    private val sessionRepository: SessionRepository,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun onCurrentPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(currentPassword = password, errorMessage = null)
    }

    fun onNewPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(newPassword = password, errorMessage = null)
    }

    fun onConfirmNewPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(confirmNewPassword = password, errorMessage = null)
    }

    fun toggleCurrentPasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isCurrentPasswordVisible = !_uiState.value.isCurrentPasswordVisible
        )
    }

    fun toggleNewPasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isNewPasswordVisible = !_uiState.value.isNewPasswordVisible
        )
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isConfirmPasswordVisible = !_uiState.value.isConfirmPasswordVisible
        )
    }

    fun submitChangePassword(onSuccess: () -> Unit) {
        val state = _uiState.value
        val current = state.currentPassword
        val newPass = state.newPassword
        val confirmPass = state.confirmNewPassword

        if (current.isBlank() || newPass.isBlank() || confirmPass.isBlank()) {
            _uiState.value = state.copy(
                errorMessage = appContext.getString(R.string.empty_fields_error)
            )
            return
        }

        // Validate: the two new entries must match
        if (newPass != confirmPass) {
            _uiState.value = state.copy(
                errorMessage = appContext.getString(R.string.password_mismatch_error)
            )
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = sessionRepository.changePassword(current, newPass)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = null)
                onSuccess()
            }.onFailure { throwable ->
                val formatted = ErrorFormatter.format(appContext, throwable)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = formatted
                )
            }
        }
    }

    companion object {
        fun provideFactory(
            sessionRepository: SessionRepository,
            context: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChangePasswordViewModel(
                    sessionRepository,
                    context.applicationContext
                ) as T
            }
        }
    }
}
