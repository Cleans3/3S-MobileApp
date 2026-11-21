package com.example.libraryapp.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.libraryapp.data.model.ApiResult
import com.example.libraryapp.data.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: LibraryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileUiState(member = repository.getCachedMember())
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getProfile()) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            member = result.data.member,
                            activeLoans = result.data.activeLoans,
                            reservationsCount = result.data.reservationsCount,
                            loanHistoryCount = result.data.loanHistoryCount,
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> {
                    if (result.code == 401) {
                        _uiState.update { it.copy(isLoading = false, isSignedOut = true) }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.message
                            )
                        }
                    }
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.logout()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isSignedOut = true,
                    member = null,
                    activeLoans = emptyList()
                )
            }
        }
    }
}
