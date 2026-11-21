package com.example.libraryapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.libraryapp.LibraryApp
import com.example.libraryapp.data.AccountException
import com.example.libraryapp.data.AccountRepository
import com.example.libraryapp.data.model.MemberProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SessionStatus { Restoring, SignedOut, SignedIn }

data class AuthUiState(
    val status: SessionStatus = SessionStatus.Restoring,
    val email: String = "",
    val password: String = "",
    val isSigningIn: Boolean = false,
    val signInError: String? = null,
    val profile: MemberProfile? = null,
    val isLoadingProfile: Boolean = false,
    val profileError: String? = null,
)

class AuthViewModel(private val repository: AccountRepository) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (repository.restoreSession()) {
                _state.update { it.copy(status = SessionStatus.SignedIn) }
                refreshProfile()
            } else {
                _state.update { it.copy(status = SessionStatus.SignedOut) }
            }
        }
    }

    fun onEmailChanged(email: String) {
        _state.update { it.copy(email = email, signInError = null) }
    }

    fun onPasswordChanged(password: String) {
        _state.update { it.copy(password = password, signInError = null) }
    }

    fun signIn() {
        val current = _state.value
        if (current.isSigningIn) return

        val email = current.email.trim()
        if (email.isEmpty() || !email.contains('@') || current.password.isEmpty()) {
            _state.update { it.copy(signInError = "Enter your email address and password.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSigningIn = true, signInError = null) }
            repository.signIn(email, current.password)
                .onSuccess { profile ->
                    _state.update {
                        it.copy(
                            isSigningIn = false,
                            password = "",
                            status = SessionStatus.SignedIn,
                            profile = profile,
                            profileError = null,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isSigningIn = false, signInError = error.message) }
                }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingProfile = true, profileError = null) }
            repository.getProfile()
                .onSuccess { profile ->
                    _state.update { it.copy(isLoadingProfile = false, profile = profile) }
                }
                .onFailure { error ->
                    if (error is AccountException.SessionExpired) {
                        _state.value = AuthUiState(
                            status = SessionStatus.SignedOut,
                            signInError = error.message,
                        )
                    } else {
                        _state.update { it.copy(isLoadingProfile = false, profileError = error.message) }
                    }
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _state.value = AuthUiState(status = SessionStatus.SignedOut)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LibraryApp
                AuthViewModel(app.container.accountRepository)
            }
        }
    }
}
