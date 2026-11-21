package vn.shelftap.app.data.repository

import kotlinx.coroutines.flow.StateFlow
import vn.shelftap.app.data.local.AuthState
import vn.shelftap.app.data.local.SessionManager
import vn.shelftap.app.data.model.AuthResponse
import vn.shelftap.app.data.model.LoginRequest
import vn.shelftap.app.data.model.LogoutRequest
import vn.shelftap.app.data.remote.ShelfTapApiService
import vn.shelftap.app.data.remote.unwrapBody

class AuthRepository(
    private val apiService: ShelfTapApiService,
    private val sessionManager: SessionManager
) {
    val authState: StateFlow<AuthState> = sessionManager.authState

    fun getDeviceId(): String = sessionManager.getDeviceId()

    fun isMustChangePassword(): Boolean = sessionManager.isMustChangePassword()

    suspend fun login(email: String, password: String): AuthResponse {
        val deviceId = sessionManager.getDeviceId()
        val request = LoginRequest(
            email = email.trim(),
            password = password,
            deviceId = deviceId
        )
        val response = apiService.login(request).unwrapBody()
        sessionManager.saveAuth(response)
        return response
    }

    suspend fun logout() {
        val refreshToken = sessionManager.getRefreshToken()
        try {
            if (!refreshToken.isNullOrEmpty()) {
                apiService.logout(LogoutRequest(refreshToken = refreshToken)).unwrapBody()
            }
        } catch (_: Exception) {
            // Server logout error shouldn't block clearing local session
        } finally {
            // Signing out tells the server and removes the session from the phone
            sessionManager.clearSession()
        }
    }

    fun clearLocalSession() {
        sessionManager.clearSession()
    }
}
