package com.example.libraryapp.ui.screens.profile

import com.example.libraryapp.data.model.BorrowedBookDto
import com.example.libraryapp.data.model.MemberDto

data class ProfileUiState(
    val isLoading: Boolean = false,
    val member: MemberDto? = null,
    val activeLoans: List<BorrowedBookDto> = emptyList(),
    val reservationsCount: Int = 0,
    val loanHistoryCount: Int = 0,
    val errorMessage: String? = null,
    val isSignedOut: Boolean = false
)
