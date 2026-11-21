package com.example.libraryapp.ui.auth

import com.example.libraryapp.data.model.MemberProfile

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loginSuccessUser: MemberProfile? = null
)
