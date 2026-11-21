package com.example.libraryapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.libraryapp.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile(forceRefresh: Boolean = false) {
        val cached = authRepository.getCachedProfile()
        if (cached != null && !forceRefresh) {
            _uiState.value = ProfileUiState.Success(cached)
        } else {
            _uiState.value = ProfileUiState.Loading
        }

        viewModelScope.launch {
            val result = authRepository.fetchProfile()
            result.fold(
                onSuccess = { profile ->
                    _uiState.value = ProfileUiState.Success(profile)
                },
                onFailure = { error ->
                    if (cached != null) {
                        // Keep displaying cached data if refresh fails
                        _uiState.value = ProfileUiState.Success(cached)
                    } else {
                        _uiState.value = ProfileUiState.Error(
                            error.message ?: "Could not load member profile"
                        )
                    }
                }
            )
        }
    }

    fun logout(onLogoutSuccess: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLogoutSuccess()
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                return ProfileViewModel(authRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
