package vn.shelftap.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.models.MemberProfileResponse
import vn.shelftap.app.data.repository.AuthRepository
import vn.shelftap.app.data.repository.AuthState

data class ProfileUiState(
    val profile: MemberProfileResponse? = null,
    val isLoading: Boolean = false,
    val isSigningOut: Boolean = false,
    val errorMessage: String? = null
)

class ProfileViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeAuthState()
        loadProfile()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.authState.collect { authState ->
                if (authState is AuthState.Authenticated) {
                    _uiState.update { it.copy(profile = authState.profile, errorMessage = null) }
                }
            }
        }
    }

    fun loadProfile() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.getProfile()
            result.fold(
                onSuccess = { profile ->
                    _uiState.update { it.copy(profile = profile, isLoading = false, errorMessage = null) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to refresh profile."
                        )
                    }
                }
            )
        }
    }

    fun signOut() {
        _uiState.update { it.copy(isSigningOut = true) }
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { it.copy(isSigningOut = false) }
        }
    }
}
