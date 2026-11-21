package com.library.account.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.library.account.data.model.MemberProfile
import com.library.account.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val profile: MemberProfile? = null,
    val errorMessage: String? = null,
    val isSignedOut: Boolean = false,
    val isMockBackend: Boolean = true,
    val showSignOutDialog: Boolean = false
)

class ProfileViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(
        profile = authRepository.getCachedProfile(),
        isMockBackend = authRepository.isMockMode()
    ))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile(forceRefresh = false)
    }

    fun loadProfile(forceRefresh: Boolean = true) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = authRepository.getProfile(forceRefresh = forceRefresh)
            result.fold(
                onSuccess = { profileData ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profile = profileData,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to refresh profile from library server."
                        )
                    }
                }
            )
        }
    }

    fun toggleSignOutDialog(show: Boolean) {
        _uiState.update { it.copy(showSignOutDialog = show) }
    }

    fun signOut() {
        _uiState.update { it.copy(showSignOutDialog = false, isLoading = true) }
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { it.copy(isLoading = false, isSignedOut = true) }
        }
    }

    fun toggleMockMode(enabled: Boolean) {
        authRepository.setMockMode(enabled)
        _uiState.update { it.copy(isMockBackend = enabled) }
        loadProfile(forceRefresh = true)
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(authRepository) as T
        }
    }
}
