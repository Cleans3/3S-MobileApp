package com.example.libraryapp.ui.profile

import com.example.libraryapp.data.model.MemberProfile

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val profile: MemberProfile) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}
