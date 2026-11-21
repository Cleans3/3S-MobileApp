package com.example.library.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.library.data.AccountRepository
import com.example.library.data.MemberProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class AccountUiState(
    val isSignedIn: Boolean = false,
    val profile: MemberProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class AccountViewModel(private val repository: AccountRepository) : ViewModel() {

    private val _state = MutableStateFlow(
        AccountUiState(
            isSignedIn = repository.isSignedIn,
            isLoading = repository.isSignedIn
        )
    )
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    init {
        if (repository.isSignedIn) refreshProfile()
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isEmpty()) {
            _state.update { it.copy(error = "Enter your email and password.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repository.signIn(email, password)
                .onSuccess { profile ->
                    _state.value = AccountUiState(isSignedIn = true, profile = profile)
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.toSignInMessage()) }
                }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repository.loadProfile()
                .onSuccess { profile ->
                    _state.update { it.copy(isSignedIn = true, profile = profile, isLoading = false) }
                }
                .onFailure { e ->
                    if (repository.isSignedIn) {
                        // Network or server problem: stay signed in and show the error.
                        _state.update { it.copy(isLoading = false, error = e.toSignInMessage()) }
                    } else {
                        // Token was rejected and the session was cleared.
                        _state.value = AccountUiState(
                            error = "Your session has expired. Please sign in again."
                        )
                    }
                }
        }
    }

    fun signOut() {
        repository.signOut()
        _state.value = AccountUiState()
    }

    companion object {
        fun factory(repository: AccountRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AccountViewModel(repository) as T
            }
    }
}

private fun Throwable.toSignInMessage(): String = when (this) {
    is HttpException -> when (code()) {
        400, 401, 403 -> "Email or password is incorrect."
        else -> "The library server returned an error (${code()})."
    }
    is IOException -> "Could not reach the library server. Check your connection."
    else -> "Something went wrong. Please try again."
}
