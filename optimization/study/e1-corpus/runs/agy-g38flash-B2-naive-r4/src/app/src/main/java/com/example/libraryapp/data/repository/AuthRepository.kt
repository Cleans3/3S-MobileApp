package com.example.libraryapp.data.repository

import com.example.libraryapp.data.local.SessionManager
import com.example.libraryapp.data.model.ApiError
import com.example.libraryapp.data.model.LoginRequest
import com.example.libraryapp.data.model.MemberProfile
import com.example.libraryapp.data.remote.LibraryApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<MemberProfile>
    suspend fun logout(): Result<Unit>
    suspend fun fetchProfile(): Result<MemberProfile>
    fun getCachedProfile(): MemberProfile?
    fun isLoggedIn(): Boolean
}

class AuthRepositoryImpl(
    private val apiService: LibraryApiService,
    private val sessionManager: SessionManager
) : AuthRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun login(email: String, password: String): Result<MemberProfile> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.login(LoginRequest(email = email.trim(), password = password))
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    sessionManager.saveAuthToken(loginResponse.accessToken)
                    loginResponse.refreshToken?.let { sessionManager.saveRefreshToken(it) }

                    // If member info is included in login response, save it
                    val profile = loginResponse.member ?: fetchProfileInternal()
                    if (profile != null) {
                        sessionManager.saveUserProfile(profile)
                        Result.success(profile)
                    } else {
                        Result.failure(Exception("Failed to retrieve member profile."))
                    }
                } else {
                    val errorMsg = parseErrorMessage(response.errorBody()?.string())
                        ?: "Sign in failed with status code ${response.code()}"
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            try {
                apiService.logout()
            } catch (_: Exception) {
                // Ignore network errors during logout
            }
            sessionManager.clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            sessionManager.clearSession()
            Result.failure(e)
        }
    }

    override suspend fun fetchProfile(): Result<MemberProfile> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMemberProfile()
            if (response.isSuccessful && response.body() != null) {
                val profile = response.body()!!
                sessionManager.saveUserProfile(profile)
                Result.success(profile)
            } else {
                val cached = sessionManager.getUserProfile()
                if (cached != null) {
                    Result.success(cached)
                } else {
                    val errorMsg = parseErrorMessage(response.errorBody()?.string())
                        ?: "Failed to load member profile (HTTP ${response.code()})"
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            val cached = sessionManager.getUserProfile()
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    private suspend fun fetchProfileInternal(): MemberProfile? {
        val response = apiService.getMemberProfile()
        return if (response.isSuccessful) response.body() else null
    }

    override fun getCachedProfile(): MemberProfile? = sessionManager.getUserProfile()

    override fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

    private fun parseErrorMessage(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val error = json.decodeFromString<ApiError>(errorBody)
            error.message
        } catch (e: Exception) {
            null
        }
    }
}
