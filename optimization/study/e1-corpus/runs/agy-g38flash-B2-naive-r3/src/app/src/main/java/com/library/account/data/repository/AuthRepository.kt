package com.library.account.data.repository

import com.library.account.data.api.LibraryApiService
import com.library.account.data.model.LoginRequest
import com.library.account.data.model.MemberProfile
import com.library.account.data.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Repository coordinating authentication, session persistence, and member profile management.
 */
class AuthRepository(
    private val apiService: LibraryApiService,
    private val sessionManager: SessionManager
) {

    private val _isLoggedIn = MutableStateFlow(sessionManager.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    /**
     * Signs in the member with email and password, persists the session, and retrieves the profile.
     */
    suspend fun login(
        email: String,
        password: String,
        staySignedIn: Boolean
    ): Result<MemberProfile> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.login(LoginRequest(email.trim(), password))
            if (response.isSuccessful && response.body() != null) {
                val loginData = response.body()!!

                // Persist session
                sessionManager.saveSession(
                    token = loginData.token,
                    email = loginData.email,
                    fullName = loginData.fullName,
                    memberNumber = loginData.memberNumber,
                    staySignedIn = staySignedIn
                )

                // Update auth state flow
                _isLoggedIn.value = true

                // Fetch full profile immediately
                fetchProfileInternal()
            } else {
                val errorMsg = when (response.code()) {
                    401 -> "Invalid library member credentials."
                    403 -> "Account has been suspended. Please contact library staff."
                    else -> "Sign-in failed with error code: ${response.code()}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage ?: "Failed to reach server"}", e))
        }
    }

    /**
     * Retrieves the member profile, falling back to cached profile if network fails.
     */
    suspend fun getProfile(forceRefresh: Boolean = false): Result<MemberProfile> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            val cached = sessionManager.getCachedProfile()
            if (cached != null) {
                return@withContext Result.success(cached)
            }
        }
        fetchProfileInternal()
    }

    private suspend fun fetchProfileInternal(): Result<MemberProfile> {
        return try {
            val response = apiService.getProfile()
            if (response.isSuccessful && response.body() != null) {
                val profile = response.body()!!
                sessionManager.saveCachedProfile(profile)
                Result.success(profile)
            } else {
                val cached = sessionManager.getCachedProfile()
                if (cached != null) {
                    Result.success(cached)
                } else {
                    Result.failure(Exception("Failed to fetch profile: ${response.code()} ${response.message()}"))
                }
            }
        } catch (e: Exception) {
            val cached = sessionManager.getCachedProfile()
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(Exception("Network error while retrieving profile.", e))
            }
        }
    }

    /**
     * Signs out the member locally and attempts notifying the library server.
     */
    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.logout()
        } catch (_: Exception) {
            // Ignore server-side logout errors; local logout takes priority
        } finally {
            sessionManager.clearSession()
            _isLoggedIn.value = false
        }
        Result.success(Unit)
    }

    fun getCachedProfile(): MemberProfile? {
        return sessionManager.getCachedProfile()
    }

    fun isMockMode(): Boolean {
        return sessionManager.isMockModeEnabled()
    }

    fun setMockMode(enabled: Boolean) {
        sessionManager.setMockModeEnabled(enabled)
    }
}
