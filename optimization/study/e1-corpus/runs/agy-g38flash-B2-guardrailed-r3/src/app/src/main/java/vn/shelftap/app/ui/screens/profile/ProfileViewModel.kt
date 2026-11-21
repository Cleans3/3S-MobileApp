package vn.shelftap.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.model.ApiResult
import vn.shelftap.app.data.model.MemberProfile
import vn.shelftap.app.data.repository.ShelfTapRepository

data class ProfileUiState(
    val profile: MemberProfile? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ProfileViewModel(
    private val repository: ShelfTapRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadCachedAndFetchProfile()
    }

    fun loadCachedAndFetchProfile(
        onSessionExpired: (() -> Unit)? = null,
        onPasswordChangeRequired: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val cached = repository.getCachedProfile()
            if (cached != null) {
                _uiState.update { it.copy(profile = cached) }
            }

            _uiState.update { it.copy(isLoading = cached == null) }

            when (val result = repository.getProfile()) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            profile = result.data,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                    if (result.data.mustChangePassword) {
                        onPasswordChangeRequired?.invoke()
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    when (result.code) {
                        ShelfTapRepository.CODE_TOKEN_INVALID -> {
                            onSessionExpired?.invoke()
                        }
                        ShelfTapRepository.CODE_PASSWORD_CHANGE_REQUIRED -> {
                            onPasswordChangeRequired?.invoke()
                        }
                        ShelfTapRepository.CODE_RATE_LIMITED -> {
                            val retry = result.retryAfterSeconds ?: 60
                            _uiState.update {
                                it.copy(errorMessage = "Too many requests. Please retry after $retry seconds.")
                            }
                        }
                        else -> {
                            _uiState.update {
                                it.copy(errorMessage = result.message ?: "Failed to load profile.")
                            }
                        }
                    }
                }
                is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Unable to connect to the server. Showing cached profile."
                        )
                    }
                }
            }
        }
    }

    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.logout()
            _uiState.update { it.copy(isLoading = false, profile = null) }
            onComplete()
        }
    }
}
