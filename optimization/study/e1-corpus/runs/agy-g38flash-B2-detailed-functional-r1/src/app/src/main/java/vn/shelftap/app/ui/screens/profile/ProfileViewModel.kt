package vn.shelftap.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.model.ErrorResponse
import vn.shelftap.app.data.model.ProfileResponse
import vn.shelftap.app.data.repository.ApiResult
import vn.shelftap.app.data.repository.AuthRepository

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isSigningOut: Boolean = false,
    val profile: ProfileResponse? = null,
    val errorMessage: String? = null
)

sealed class ProfileNavigationEvent {
    object NavigateToSignIn : ProfileNavigationEvent()
    object NavigateToChangePassword : ProfileNavigationEvent()
}

class ProfileViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<ProfileNavigationEvent>()
    val navigationEvent: SharedFlow<ProfileNavigationEvent> = _navigationEvent.asSharedFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.getProfile()) {
                is ApiResult.Success -> {
                    val profile = result.data
                    _uiState.update { it.copy(isLoading = false, profile = profile) }
                    if (profile.mustChangePassword) {
                        _navigationEvent.emit(ProfileNavigationEvent.NavigateToChangePassword)
                    }
                }
                is ApiResult.Error -> {
                    val error = result.error
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                    if (error.code == ErrorResponse.CODE_PASSWORD_CHANGE_REQUIRED) {
                        _navigationEvent.emit(ProfileNavigationEvent.NavigateToChangePassword)
                    } else if (error.code == ErrorResponse.CODE_TOKEN_INVALID) {
                        _navigationEvent.emit(ProfileNavigationEvent.NavigateToSignIn)
                    }
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningOut = true) }
            authRepository.logout()
            _uiState.update { it.copy(isSigningOut = false) }
            _navigationEvent.emit(ProfileNavigationEvent.NavigateToSignIn)
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(authRepository) as T
        }
    }
}
