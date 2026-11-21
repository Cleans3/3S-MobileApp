package com.example.library.data

import com.example.library.data.api.LibraryApi
import com.example.library.data.model.LoginRequest
import com.example.library.data.model.MemberProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Single source of truth for sign-in state and the member's account data. */
class AuthRepository(
    private val api: LibraryApi,
    private val tokenStore: TokenStore
) {

    /** Emits true while a session token is stored on the device. */
    val isSignedIn: Flow<Boolean> = tokenStore.token
        .map { !it.isNullOrBlank() }
        .distinctUntilChanged()

    /** Exchanges email and password for a session token and stores it. */
    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        val token = api.login(LoginRequest(email = email.trim(), password = password)).token
        require(!token.isNullOrBlank()) { "Server returned no session token" }
        tokenStore.save(token)
    }

    suspend fun fetchProfile(): Result<MemberProfile> = runCatching {
        api.getMyProfile()
    }

    /** Clears the local session. The server call is best effort. */
    suspend fun signOut() {
        runCatching { api.logout() }
        tokenStore.clear()
    }
}
