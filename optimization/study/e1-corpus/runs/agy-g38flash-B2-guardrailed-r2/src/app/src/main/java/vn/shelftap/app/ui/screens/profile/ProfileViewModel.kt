package vn.shelftap.app.ui.screens.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.shelftap.app.data.api.models.MemberProfileResponse
import vn.shelftap.app.data.repository.SessionRepository
import vn.shelftap.app.ui.common.ErrorFormatter

data class ProfileUiState(
    val profile: MemberProfileResponse? = null,
    val isLoading: Boolean = false,
    val isSigningOut: Boolean = false,
    val errorMessage: String? = null
)

class ProfileViewModel(
    private val sessionRepository: SessionRepository,
    private val appContext: Context,
    initialProfile: MemberProfileResponse?
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(profile = initialProfile))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        refreshProfile()
    }

    fun updateProfile(profile: MemberProfileResponse) {
        _uiState.value = _uiState.value.copy(profile = profile)
    }

    fun refreshProfile() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = sessionRepository.refreshProfile()
            result.onSuccess { profile ->
                _uiState.value = _uiState.value.copy(
                    profile = profile,
                    isLoading = false,
                    errorMessage = null
                )
            }.onFailure { throwable ->
                val formatted = ErrorFormatter.format(appContext, throwable)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = formatted
                )
            }
        }
    }

    fun signOut() {
        _uiState.value = _uiState.value.copy(isSigningOut = true)
        viewModelScope.launch {
            sessionRepository.logout()
            _uiState.value = _uiState.value.copy(isSigningOut = false)
        }
    }

    companion object {
        fun provideFactory(
            sessionRepository: SessionRepository,
            context: Context,
            initialProfile: MemberProfileResponse?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(
                    sessionRepository,
                    context.applicationContext,
                    initialProfile
                ) as T
            }
        }
    }
}
