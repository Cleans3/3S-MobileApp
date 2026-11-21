package vn.shelftap.app.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.shelftap.app.data.api.ApiClient
import vn.shelftap.app.data.api.ShelfTapApiService
import vn.shelftap.app.data.local.PreferenceManager
import vn.shelftap.app.data.model.ApiException
import vn.shelftap.app.data.model.AuthResponse
import vn.shelftap.app.data.model.ChangePasswordRequest
import vn.shelftap.app.data.model.LoginRequest
import vn.shelftap.app.data.model.LogoutRequest
import vn.shelftap.app.data.model.MemberProfile

sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data object MustChangePassword : AuthState
    data class Authenticated(val profile: MemberProfile? = null) : AuthState
}

class AuthRepository(
    private val preferenceManager: PreferenceManager
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val apiService: ShelfTapApiService = ApiClient.createApiService(
        preferenceManager = preferenceManager,
        onSessionExpired = {
            _authState.value = AuthState.Unauthenticated
        }
    )

    init {
        checkInitialAuthState()
    }

    fun checkInitialAuthState() {
        if (!preferenceManager.isLoggedIn()) {
            _authState.value = AuthState.Unauthenticated
            return
        }

        if (preferenceManager.mustChangePassword()) {
            _authState.value = AuthState.MustChangePassword
            return
        }

        // Validate session and retrieve member profile
        _authState.value = AuthState.Authenticated(null)
        repositoryScope.launch {
            loadProfile()
        }
    }

    suspend fun login(email: String, password: String): Result<AuthResponse> =
        withContext(Dispatchers.IO) {
            try {
                val deviceId = preferenceManager.getOrCreateDeviceId()
                val response = apiService.login(
                    LoginRequest(
                        email = email.trim(),
                        password = password,
                        deviceId = deviceId
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val auth = response.body()!!
                    preferenceManager.saveTokens(
                        accessToken = auth.accessToken,
                        refreshToken = auth.refreshToken,
                        mustChangePassword = auth.mustChangePassword
                    )

                    if (auth.mustChangePassword) {
                        _authState.value = AuthState.MustChangePassword
                    } else {
                        _authState.value = AuthState.Authenticated(null)
                        loadProfile()
                    }

                    Result.success(auth)
                } else {
                    Result.failure(ApiClient.parseError(response))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun loadProfile(): Result<MemberProfile> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getMe()
                if (response.isSuccessful && response.body() != null) {
                    val profile = response.body()!!
                    if (profile.mustChangePassword) {
                        preferenceManager.setMustChangePassword(true)
                        _authState.value = AuthState.MustChangePassword
                    } else {
                        preferenceManager.setMustChangePassword(false)
                        _authState.value = AuthState.Authenticated(profile)
                    }
                    Result.success(profile)
                } else {
                    Result.failure(ApiClient.parseError(response))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.changePassword(
                    ChangePasswordRequest(
                        currentPassword = currentPassword,
                        newPassword = newPassword
                    )
                )

                if (response.isSuccessful) {
                    // Password changed successfully: clear mandatory flag
                    preferenceManager.setMustChangePassword(false)
                    _authState.value = AuthState.Authenticated(null)
                    loadProfile()
                    Result.success(Unit)
                } else {
                    Result.failure(ApiClient.parseError(response))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun logout(): Result<Unit> =
        withContext(Dispatchers.IO) {
            val refreshToken = preferenceManager.getRefreshToken()
            try {
                if (!refreshToken.isNullOrBlank()) {
                    apiService.logout(LogoutRequest(refreshToken = refreshToken))
                }
            } catch (_: Exception) {
                // Ignore network error on logout so session is still removed locally
            } finally {
                preferenceManager.clearSession()
                _authState.value = AuthState.Unauthenticated
            }
            Result.success(Unit)
        }
}
