package com.example.library.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.library.data.AuthRepository
import com.example.library.data.isUnauthorized
import com.example.library.data.model.MemberProfile
import com.example.library.data.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    /** null while the stored session is still being read. */
    val signedIn: Boolean? = null,
    val email: String = "",
    val password: String = "",
    val signingIn: Boolean = false,
    val signInError: String? = null,
    val profile: MemberProfile? = null,
    val profileLoading: Boolean = false,
    val profileError: String? = null
)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.isSignedIn.collect { signedIn ->
                _uiState.update { state ->
                    state.copy(
                        signedIn = signedIn,
                        profile = if (signedIn) state.profile else null,
                        profileError = null
                    )
                }
                if (signedIn && _uiState.value.profile == null) {
                    refreshProfile()
                }
            }
        }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, signInError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, signInError = null) }
    }

    fun signIn() {
        val state = _uiState.value
        if (state.signingIn) return
        if (state.email.isBlank() || state.password.isEmpty()) {
            _uiState.update { it.copy(signInError = "Enter your email and password.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(signingIn = true, signInError = null) }
            val result = repository.signIn(state.email, state.password)
            _uiState.update { current ->
                result.fold(
                    onSuccess = { current.copy(signingIn = false, password = "") },
                    onFailure = { error ->
                        current.copy(
                            signingIn = false,
                            signInError = if (error.isUnauthorized()) {
                                "Email or password is incorrect."
                            } else {
                                error.userMessage()
                            }
                        )
                    }
                )
            }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(profileLoading = true, profileError = null) }
            val result = repository.fetchProfile()
            result.fold(
                onSuccess = { profile ->
                    _uiState.update { it.copy(profile = profile, profileLoading = false) }
                },
                onFailure = { error ->
                    if (error.isUnauthorized()) {
                        // The stored token was rejected: end the session and show sign-in.
                        repository.signOut()
                    } else {
                        _uiState.update {
                            it.copy(profileLoading = false, profileError = error.userMessage())
                        }
                    }
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
        }
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(repository) as T
        }
    }
}
