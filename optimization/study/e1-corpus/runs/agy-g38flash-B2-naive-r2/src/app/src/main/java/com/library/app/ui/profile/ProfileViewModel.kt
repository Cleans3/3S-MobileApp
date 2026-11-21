package com.library.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.library.app.data.model.MemberProfile
import com.library.app.data.repository.LibraryRepository
import com.library.app.data.repository.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: MemberProfile? = null,
    val isLoading: Boolean = false,
    val isSigningOut: Boolean = false,
    val errorMessage: String? = null
)

class ProfileViewModel(
    private val repository: LibraryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        val cached = repository.getCachedProfile()
        if (cached != null) {
            _uiState.value = _uiState.value.copy(profile = cached)
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = cached == null)
            when (val result = repository.fetchProfile()) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        profile = result.data,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                Resource.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSigningOut = true)
            repository.signOut()
            _uiState.value = _uiState.value.copy(isSigningOut = false)
            onSignedOut()
        }
    }
}
