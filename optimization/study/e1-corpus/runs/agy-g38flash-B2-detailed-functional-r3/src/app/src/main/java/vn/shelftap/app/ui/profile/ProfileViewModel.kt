package vn.shelftap.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.shelftap.app.data.model.ApiException
import vn.shelftap.app.data.model.MemberProfile
import vn.shelftap.app.data.repository.AuthRepository

data class ProfileUiState(
    val isLoading: Boolean = false,
    val profile: MemberProfile? = null,
    val errorMessage: String? = null,
    val isSigningOut: Boolean = false
)

class ProfileViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = authRepository.loadProfile()
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    profile = result.getOrNull(),
                    errorMessage = null
                )
            } else {
                val error = result.exceptionOrNull()
                val msg = if (error is ApiException) {
                    error.error.detail ?: error.error.title ?: "Failed to load profile."
                } else {
                    error?.localizedMessage ?: "Failed to load profile."
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = msg
                )
            }
        }
    }

    fun signOut() {
        _uiState.value = _uiState.value.copy(isSigningOut = true)
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = _uiState.value.copy(isSigningOut = false)
        }
    }

    companion object {
        fun provideFactory(authRepository: AuthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProfileViewModel(authRepository) as T
                }
            }
    }
}
