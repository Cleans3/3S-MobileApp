package vn.shelftap.app.ui.screens.signin

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

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class SignInViewModel(
    private val sessionRepository: SessionRepository,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(email = email, errorMessage = null)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(password = password, errorMessage = null)
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(isPasswordVisible = !_uiState.value.isPasswordVisible)
    }

    fun setInitialMessage(message: String?) {
        if (!message.isNullOrBlank() && _uiState.value.errorMessage == null) {
            _uiState.value = _uiState.value.copy(errorMessage = message)
        }
    }

    fun signIn() {
        val state = _uiState.value
        val email = state.email.trim()
        val password = state.password

        if (email.isEmpty() || password.isEmpty()) {
            _uiState.value = state.copy(
                errorMessage = appContext.getString(R.string.empty_fields_error)
            )
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = sessionRepository.login(email, password)
            result.onFailure { throwable ->
                val formatted = ErrorFormatter.format(appContext, throwable)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = formatted
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = null)
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
                return SignInViewModel(sessionRepository, context.applicationContext) as T
            }
        }
    }
}
