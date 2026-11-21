package com.example.libraryapp.data.repository

import com.example.libraryapp.data.local.SessionManager
import com.example.libraryapp.data.model.ApiResult
import com.example.libraryapp.data.model.AuthResponse
import com.example.libraryapp.data.model.LoginRequest
import com.example.libraryapp.data.model.LogoutResponse
import com.example.libraryapp.data.model.MemberDto
import com.example.libraryapp.data.model.MemberProfileResponse
import com.example.libraryapp.data.remote.LibraryApiService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.IOException

interface LibraryRepository {
    val isLoggedInFlow: StateFlow<Boolean>
    fun isLoggedIn(): Boolean
    fun getCachedMember(): MemberDto?
    suspend fun login(email: String, password: String): ApiResult<AuthResponse>
    suspend fun logout(): ApiResult<LogoutResponse>
    suspend fun getProfile(): ApiResult<MemberProfileResponse>
    fun clearSession()
}

class LibraryRepositoryImpl(
    private val apiService: LibraryApiService,
    private val sessionManager: SessionManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LibraryRepository {

    override val isLoggedInFlow: StateFlow<Boolean> = sessionManager.isLoggedInFlow

    override fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

    override fun getCachedMember(): MemberDto? = sessionManager.getMember()

    override suspend fun login(email: String, password: String): ApiResult<AuthResponse> =
        withContext(ioDispatcher) {
            try {
                val response = apiService.login(LoginRequest(email = email, password = password))
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        sessionManager.saveAuthToken(body.token)
                        sessionManager.saveMember(body.member)
                        ApiResult.Success(body)
                    } else {
                        ApiResult.Error("Empty response from server")
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        401 -> "Invalid email or password"
                        403 -> "Account is locked or suspended"
                        else -> "Sign-in failed with error code ${response.code()}"
                    }
                    ApiResult.Error(errorMsg, response.code())
                }
            } catch (e: IOException) {
                ApiResult.Error("Network error. Unable to connect to library server.")
            } catch (e: Exception) {
                ApiResult.Error(e.localizedMessage ?: "Unexpected error during sign-in")
            }
        }

    override suspend fun getProfile(): ApiResult<MemberProfileResponse> =
        withContext(ioDispatcher) {
            try {
                val response = apiService.getMemberProfile()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        sessionManager.saveMember(body.member)
                        ApiResult.Success(body)
                    } else {
                        ApiResult.Error("Empty profile response from server")
                    }
                } else {
                    if (response.code() == 401) {
                        sessionManager.clearSession()
                        ApiResult.Error("Session expired. Please sign in again.", 401)
                    } else {
                        ApiResult.Error("Failed to fetch profile (Code ${response.code()})", response.code())
                    }
                }
            } catch (e: IOException) {
                ApiResult.Error("Network error. Unable to load profile.")
            } catch (e: Exception) {
                ApiResult.Error(e.localizedMessage ?: "Unexpected error fetching profile")
            }
        }

    override suspend fun logout(): ApiResult<LogoutResponse> =
        withContext(ioDispatcher) {
            try {
                val response = apiService.logout()
                sessionManager.clearSession()
                if (response.isSuccessful && response.body() != null) {
                    ApiResult.Success(response.body()!!)
                } else {
                    ApiResult.Success(LogoutResponse(success = true, message = "Signed out locally"))
                }
            } catch (e: Exception) {
                // Always clear local session even if remote logout fails
                sessionManager.clearSession()
                ApiResult.Success(LogoutResponse(success = true, message = "Signed out locally"))
            }
        }

    override fun clearSession() {
        sessionManager.clearSession()
    }
}
