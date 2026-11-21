package vn.shelftap.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import retrofit2.Response
import vn.shelftap.app.data.api.ShelfTapApiService
import vn.shelftap.app.data.local.EncryptedSessionStorage
import vn.shelftap.app.data.models.ApiErrorResponse
import vn.shelftap.app.data.models.AppError
import vn.shelftap.app.data.models.AuthTokenResponse
import vn.shelftap.app.data.models.ChangePasswordRequest
import vn.shelftap.app.data.models.LoginRequest
import vn.shelftap.app.data.models.LogoutRequest
import vn.shelftap.app.data.models.MemberProfileResponse
import java.io.IOException

sealed class AuthState {
    data object Loading : AuthState()
    data object Unauthenticated : AuthState()
    data class RequirePasswordChange(val email: String) : AuthState()
    data class Authenticated(val profile: MemberProfileResponse) : AuthState()
}

class AuthRepository(
    private val apiService: ShelfTapApiService,
    private val sessionStorage: EncryptedSessionStorage,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    suspend fun checkInitialSession(): AuthState = withContext(ioDispatcher) {
        val refreshToken = sessionStorage.getRefreshToken()
        if (refreshToken.isNullOrBlank()) {
            _authState.value = AuthState.Unauthenticated
            return@withContext AuthState.Unauthenticated
        }

        val mustChange = sessionStorage.mustChangePassword()
        if (mustChange) {
            val cachedProfile = sessionStorage.getCachedProfile()
            val state = AuthState.RequirePasswordChange(cachedProfile?.email ?: "")
            _authState.value = state
            return@withContext state
        }

        // Validate session by loading profile
        val profileResult = getProfile()
        val finalState = profileResult.fold(
            onSuccess = { profile ->
                if (profile.mustChangePassword) {
                    AuthState.RequirePasswordChange(profile.email)
                } else {
                    AuthState.Authenticated(profile)
                }
            },
            onFailure = { error ->
                if (error is AppError.TokenInvalid) {
                    sessionStorage.clearSession()
                    AuthState.Unauthenticated
                } else {
                    // If network issue, use cached profile if available
                    val cached = sessionStorage.getCachedProfile()
                    if (cached != null) {
                        AuthState.Authenticated(cached)
                    } else {
                        AuthState.Unauthenticated
                    }
                }
            }
        )

        _authState.value = finalState
        finalState
    }

    suspend fun login(email: String, password: String): Result<AuthTokenResponse> = withContext(ioDispatcher) {
        try {
            val deviceId = sessionStorage.getOrCreateDeviceId()
            val response = apiService.login(LoginRequest(email.trim(), password, deviceId))

            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!
                sessionStorage.saveAuthTokens(
                    accessToken = tokenResponse.accessToken,
                    refreshToken = tokenResponse.refreshToken,
                    mustChangePassword = tokenResponse.mustChangePassword
                )

                if (tokenResponse.mustChangePassword) {
                    _authState.value = AuthState.RequirePasswordChange(email)
                } else {
                    val profileRes = getProfile()
                    val profile = profileRes.getOrNull()
                    if (profile != null) {
                        _authState.value = AuthState.Authenticated(profile)
                    } else {
                        // Profile fetch fallback using minimal model
                        val minimalProfile = MemberProfileResponse(
                            memberId = "",
                            fullName = "",
                            email = email,
                            memberType = "",
                            status = "",
                            historyEnabled = false,
                            mustChangePassword = false
                        )
                        _authState.value = AuthState.Authenticated(minimalProfile)
                    }
                }
                Result.success(tokenResponse)
            } else {
                val appError = parseErrorResponse(response.errorBody(), response.code(), response.headers())
                if (appError is AppError.PasswordChangeRequired) {
                    _authState.value = AuthState.RequirePasswordChange(email)
                }
                Result.failure(appError)
            }
        } catch (e: IOException) {
            Result.failure(AppError.Network("Unable to reach server. Please check your network connection."))
        } catch (e: Exception) {
            Result.failure(AppError.Unknown(null, e.localizedMessage ?: "Sign in failed."))
        }
    }

    suspend fun getProfile(): Result<MemberProfileResponse> = withContext(ioDispatcher) {
        try {
            val response = apiService.getProfile()
            if (response.isSuccessful && response.body() != null) {
                val profile = response.body()!!
                sessionStorage.saveProfile(profile)
                Result.success(profile)
            } else {
                val appError = parseErrorResponse(response.errorBody(), response.code(), response.headers())
                if (appError is AppError.TokenInvalid) {
                    handleSessionExpired()
                }
                Result.failure(appError)
            }
        } catch (e: IOException) {
            Result.failure(AppError.Network("Unable to connect to server."))
        } catch (e: Exception) {
            Result.failure(AppError.Unknown(null, e.localizedMessage ?: "Failed to retrieve profile."))
        }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val response = apiService.changePassword(
                ChangePasswordRequest(currentPassword = currentPassword, newPassword = newPassword)
            )

            if (response.isSuccessful) {
                sessionStorage.setMustChangePassword(false)
                // Refresh profile after password change
                val profileRes = getProfile()
                profileRes.onSuccess { profile ->
                    _authState.value = AuthState.Authenticated(profile)
                }
                Result.success(Unit)
            } else {
                val appError = parseErrorResponse(response.errorBody(), response.code(), response.headers())
                Result.failure(appError)
            }
        } catch (e: IOException) {
            Result.failure(AppError.Network("Unable to connect to server."))
        } catch (e: Exception) {
            Result.failure(AppError.Unknown(null, e.localizedMessage ?: "Password change failed."))
        }
    }

    suspend fun logout(): Result<Unit> = withContext(ioDispatcher) {
        try {
            val refreshToken = sessionStorage.getRefreshToken()
            if (!refreshToken.isNullOrBlank()) {
                apiService.logout(LogoutRequest(refreshToken))
            }
        } catch (_: Exception) {
            // Server error or offline during logout should still clear local session
        } finally {
            sessionStorage.clearSession()
            _authState.value = AuthState.Unauthenticated
        }
        Result.success(Unit)
    }

    fun handleSessionExpired() {
        kotlinx.coroutines.CoroutineScope(ioDispatcher).run {
            sessionStorage.clearSession()
            _authState.value = AuthState.Unauthenticated
        }
    }

    private fun parseErrorResponse(
        errorBody: ResponseBody?,
        httpStatusCode: Int,
        headers: okhttp3.Headers
    ): AppError {
        val errorJson = errorBody?.string()
        val errorObj = if (!errorJson.isNullOrBlank()) {
            try {
                json.decodeFromString<ApiErrorResponse>(errorJson)
            } catch (_: Exception) {
                null
            }
        } else null

        val code = errorObj?.code ?: ""
        val detail = errorObj?.detail ?: errorObj?.title

        return when {
            code == "INVALID_CREDENTIALS" || httpStatusCode == 401 && code == "INVALID_CREDENTIALS" -> {
                AppError.InvalidCredentials(detail ?: "Invalid email or password.")
            }
            code == "ACCOUNT_LOCKED" || httpStatusCode == 423 -> {
                AppError.AccountLocked(detail ?: "Your account is locked. Please contact library staff.")
            }
            code == "RATE_LIMITED" || httpStatusCode == 429 -> {
                val retryAfterSeconds = headers["Retry-After"]?.toLongOrNull()
                val msg = if (retryAfterSeconds != null) {
                    "Too many attempts. Please try again after $retryAfterSeconds seconds."
                } else {
                    detail ?: "Too many attempts. Please try again later."
                }
                AppError.RateLimited(retryAfterSeconds, msg)
            }
            code == "TOKEN_INVALID" -> {
                AppError.TokenInvalid(detail ?: "Your session has expired. Please sign in again.")
            }
            code == "PASSWORD_CHANGE_REQUIRED" || httpStatusCode == 403 && code == "PASSWORD_CHANGE_REQUIRED" -> {
                AppError.PasswordChangeRequired(detail ?: "Password change is required before proceeding.")
            }
            else -> {
                AppError.Unknown(code, detail ?: "An error occurred (HTTP $httpStatusCode).")
            }
        }
    }
}
