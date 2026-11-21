package com.example.libraryapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.libraryapp.data.AccountRepository
import com.example.libraryapp.data.api.MemberProfile
import com.example.libraryapp.data.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: MemberProfile? = null,
    val error: String? = null
)

class ProfileViewModel(private val repository: AccountRepository) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    fun load() {
        _state.update { ProfileUiState(isLoading = true) }
        viewModelScope.launch {
            try {
                val profile = repository.currentProfile()
                _state.update { ProfileUiState(isLoading = false, profile = profile) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { ProfileUiState(isLoading = false, error = e.toUserMessage()) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
        }
    }
}
