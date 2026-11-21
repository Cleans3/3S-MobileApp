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
import vn.shelftap.app.data.model.MemberProfile
import vn.shelftap.app.data.remote.ApiException
import vn.shelftap.app.data.remote.ErrorCodes
import vn.shelftap.app.data.remote.toUserMessage
import vn.shelftap.app.data.repository.AuthRepository
import vn.shelftap.app.data.repository.MemberRepository

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isLoggingOut: Boolean = false,
    val profile: MemberProfile? = null,
    val errorMessage: String? = null
)

sealed interface ProfileNavigationEvent {
    data object NavigateToSignIn : ProfileNavigationEvent
    data object NavigateToChangePassword : ProfileNavigationEvent
}

class ProfileViewModel(
    private val memberRepository: MemberRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _navEvents = MutableSharedFlow<ProfileNavigationEvent>()
    val navEvents: SharedFlow<ProfileNavigationEvent> = _navEvents.asSharedFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val profile = memberRepository.getProfile()
                _uiState.update { it.copy(isLoading = false, profile = profile) }

                if (profile.mustChangePassword) {
                    _navEvents.emit(ProfileNavigationEvent.NavigateToChangePassword)
                }
            } catch (e: ApiException) {
                val message = when (e.error.code) {
                    ErrorCodes.ACCOUNT_LOCKED -> {
                        e.error.detail ?: "Your account has been locked. Please contact library support."
                    }
                    ErrorCodes.RATE_LIMITED -> {
                        val seconds = e.error.retryAfterSeconds
                        if (seconds != null) {
                            "Rate limited. Please wait $seconds seconds before retrying."
                        } else {
                            e.error.detail ?: "Rate limit exceeded. Please wait a moment."
                        }
                    }
                    ErrorCodes.PASSWORD_CHANGE_REQUIRED -> {
                        _navEvents.emit(ProfileNavigationEvent.NavigateToChangePassword)
                        e.error.detail ?: "You must change your password."
                    }
                    ErrorCodes.TOKEN_INVALID -> {
                        // Token refresh failed, user returned to sign-in
                        _navEvents.emit(ProfileNavigationEvent.NavigateToSignIn)
                        "Session expired. Please sign in again."
                    }
                    else -> e.error.toUserMessage("Failed to load profile.")
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = message) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load profile. Please check your network connection."
                    )
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true) }
            try {
                authRepository.logout()
            } catch (_: Exception) {
                // Guaranteed removal from phone is handled by repository
            } finally {
                _uiState.update { it.copy(isLoggingOut = false) }
                _navEvents.emit(ProfileNavigationEvent.NavigateToSignIn)
            }
        }
    }

    class Factory(
        private val memberRepository: MemberRepository,
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(memberRepository, authRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
