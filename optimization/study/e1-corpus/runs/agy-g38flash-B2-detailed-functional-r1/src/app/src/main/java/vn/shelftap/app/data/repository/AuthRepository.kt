package vn.shelftap.app.data.repository

import retrofit2.Response
import vn.shelftap.app.data.api.ShelfTapApiService
import vn.shelftap.app.data.local.DeviceIdProvider
import vn.shelftap.app.data.local.SessionManager
import vn.shelftap.app.data.model.ChangePasswordRequest
import vn.shelftap.app.data.model.ErrorResponse
import vn.shelftap.app.data.model.LoginRequest
import vn.shelftap.app.data.model.LoginResponse
import vn.shelftap.app.data.model.LogoutRequest
import vn.shelftap.app.data.model.ProfileResponse
import vn.shelftap.app.data.model.ShelfTapError

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val error: ShelfTapError) : ApiResult<Nothing>()
}

class AuthRepository(
    private val apiService: ShelfTapApiService,
    private val sessionManager: SessionManager,
    private val deviceIdProvider: DeviceIdProvider
) {

    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

    fun mustChangePassword(): Boolean = sessionManager.mustChangePassword()

    suspend fun login(email: String, password: String): ApiResult<LoginResponse> {
        return try {
            val deviceId = deviceIdProvider.getDeviceId()
            val response = apiService.login(LoginRequest(email = email, password = password, deviceId = deviceId))
            if (response.isSuccessful && response.body() != null) {
                val loginData = response.body()!!
                sessionManager.saveSession(loginData)
                ApiResult.Success(loginData)
            } else {
                val error = parseError(response)
                ApiResult.Error(error)
            }
        } catch (e: Exception) {
            ApiResult.Error(ShelfTapError.networkError(e))
        }
    }

    suspend fun logout(): ApiResult<Unit> {
        return try {
            val currentRefreshToken = sessionManager.getRefreshToken()
            if (!currentRefreshToken.isNullOrBlank()) {
                try {
                    apiService.logout(LogoutRequest(refreshToken = currentRefreshToken))
                } catch (_: Exception) {
                    // Ignore network failure on logout; ensure local session is cleared
                }
            }
            sessionManager.clearSession()
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            sessionManager.clearSession()
            ApiResult.Success(Unit)
        }
    }

    suspend fun getProfile(): ApiResult<ProfileResponse> {
        return try {
            val response = apiService.getProfile()
            if (response.isSuccessful && response.body() != null) {
                val profile = response.body()!!
                if (profile.mustChangePassword) {
                    sessionManager.setMustChangePassword(true)
                }
                ApiResult.Success(profile)
            } else {
                val error = parseError(response)
                if (error.code == ErrorResponse.CODE_PASSWORD_CHANGE_REQUIRED) {
                    sessionManager.setMustChangePassword(true)
                }
                ApiResult.Error(error)
            }
        } catch (e: Exception) {
            ApiResult.Error(ShelfTapError.networkError(e))
        }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<Unit> {
        return try {
            val response = apiService.changePassword(
                ChangePasswordRequest(currentPassword = currentPassword, newPassword = newPassword)
            )
            if (response.isSuccessful) {
                sessionManager.setMustChangePassword(false)
                ApiResult.Success(Unit)
            } else {
                val error = parseError(response)
                ApiResult.Error(error)
            }
        } catch (e: Exception) {
            ApiResult.Error(ShelfTapError.networkError(e))
        }
    }

    private fun <T> parseError(response: Response<T>): ShelfTapError {
        val statusCode = response.code()
        val retryAfterHeader = response.headers()["Retry-After"]
        val errorJson = try {
            response.errorBody()?.string()
        } catch (e: Exception) {
            null
        }
        val errorResponse = ErrorResponse.parse(errorJson)
        return ShelfTapError.fromErrorResponse(errorResponse, statusCode, retryAfterHeader)
    }
}
