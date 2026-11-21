package com.example.libraryapp.ui.screens.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.libraryapp.data.model.ApiResult
import com.example.libraryapp.data.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repository: LibraryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChanged(newEmail: String) {
        _uiState.update {
            it.copy(
                email = newEmail.trim(),
                emailError = null,
                generalError = null
            )
        }
    }

    fun onPasswordChanged(newPassword: String) {
        _uiState.update {
            it.copy(
                password = newPassword,
                passwordError = null,
                generalError = null
            )
        }
    }

    fun togglePasswordVisibility() {
        _uiState.update {
            it.copy(isPasswordVisible = !it.isPasswordVisible)
        }
    }

    fun signIn() {
        val email = _uiState.value.email
        val password = _uiState.value.password

        var isValid = true
        if (email.isBlank() || !isValidEmail(email)) {
            _uiState.update { it.copy(emailError = "Please enter a valid library email address") }
            isValid = false
        }

        if (password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Password cannot be empty") }
            isValid = false
        } else if (password.length < 4) {
            _uiState.update { it.copy(passwordError = "Password must be at least 4 characters") }
            isValid = false
        }

        if (!isValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            when (val result = repository.login(email, password)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, isSuccess = true, generalError = null)
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, generalError = result.message)
                    }
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun fillDemoCredentials() {
        _uiState.update {
            it.copy(
                email = "eleanor.vance@citylibrary.org",
                password = "libraryPass2024",
                emailError = null,
                passwordError = null,
                generalError = null
            )
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }
}
