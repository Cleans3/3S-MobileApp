package vn.shelftap.app.data.repository

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import vn.shelftap.app.data.api.ShelfTapApiService
import vn.shelftap.app.data.api.TokenProvider
import vn.shelftap.app.data.api.models.ApiErrorCodes
import vn.shelftap.app.data.api.models.ApiErrorResponse
import vn.shelftap.app.data.api.models.ApiException
import vn.shelftap.app.data.api.models.ChangePasswordRequest
import vn.shelftap.app.data.api.models.LoginRequest
import vn.shelftap.app.data.api.models.LogoutRequest
import vn.shelftap.app.data.api.models.MemberProfileResponse
import vn.shelftap.app.data.api.models.RefreshRequest
import vn.shelftap.app.data.api.models.TokenResponse
import vn.shelftap.app.data.api.models.UserSession
import vn.shelftap.app.data.security.DeviceIdProvider
import vn.shelftap.app.data.security.KeystoreCrypto
import java.io.File

sealed interface SessionState {
    data object Loading : SessionState
    data class LoggedOut(val message: String? = null) : SessionState
    data object MustChangePassword : SessionState
    data class Authenticated(val profile: MemberProfileResponse) : SessionState
}

class SessionRepository(
    private val context: Context,
    private val crypto: KeystoreCrypto,
    private val deviceIdProvider: DeviceIdProvider,
    private val apiService: ShelfTapApiService,
    private val rawOkHttpClient: OkHttpClient,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher
) : TokenProvider {

    private val sessionFile = File(context.filesDir, "session.enc")
    private val refreshLock = Any()

    @Volatile
    private var currentSession: UserSession? = null

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    suspend fun initialize() = withContext(ioDispatcher) {
        val loaded = loadSessionFromDisk()
        currentSession = loaded
        if (loaded != null) {
            if (loaded.mustChangePassword) {
                _sessionState.value = SessionState.MustChangePassword
            } else if (loaded.profile != null) {
                _sessionState.value = SessionState.Authenticated(loaded.profile)
                // Refresh profile in background
                refreshProfileSilently()
            } else {
                // Have tokens, need profile
                val profileResult = runCatching { fetchProfileInternal() }
                val profile = profileResult.getOrNull()
                if (profile != null) {
                    if (profile.mustChangePassword) {
                        saveSessionToDisk(loaded.copy(mustChangePassword = true, profile = profile))
                        _sessionState.value = SessionState.MustChangePassword
                    } else {
                        saveSessionToDisk(loaded.copy(profile = profile))
                        _sessionState.value = SessionState.Authenticated(profile)
                    }
                } else {
                    // Profile fetch failed. If refresh failed, session is already cleared
                    if (currentSession == null) {
                        _sessionState.value = SessionState.LoggedOut("Session expired. Please sign in again.")
                    }
                }
            }
        } else {
            _sessionState.value = SessionState.LoggedOut()
        }
    }

    override fun getAccessToken(): String? = currentSession?.accessToken

    /**
     * Synchronously refreshes the token.
     * Prevents duplicate refreshes by checking if the token was already refreshed
     * by another concurrent call while waiting for the lock.
     */
    override fun refreshTokenSynchronized(failedToken: String?): String? {
        synchronized(refreshLock) {
            val session = currentSession ?: loadSessionFromDisk() ?: return null

            // If token has already been refreshed by another thread
            if (failedToken != null && session.accessToken != failedToken) {
                currentSession = session
                return session.accessToken
            }

            val deviceId = deviceIdProvider.getDeviceIdSync()
            val refreshPayload = json.encodeToString(RefreshRequest(session.refreshToken, deviceId))
            val request = Request.Builder()
                .url("https://api.shelftap.example/api/auth/refresh")
                .post(refreshPayload.toRequestBody("application/json".toMediaType()))
                .build()

            val response = try {
                rawOkHttpClient.newCall(request).execute()
            } catch (e: Exception) {
                clearSessionSync()
                return null
            }

            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                response.close()
                val tokenResponse = try {
                    json.decodeFromString<TokenResponse>(body)
                } catch (e: Exception) {
                    clearSessionSync()
                    return null
                }

                val updatedSession = session.copy(
                    accessToken = tokenResponse.accessToken,
                    accessExpiresIn = tokenResponse.accessExpiresIn,
                    refreshToken = tokenResponse.refreshToken,
                    mustChangePassword = tokenResponse.mustChangePassword
                )
                saveSessionToDiskSync(updatedSession)
                currentSession = updatedSession
                return tokenResponse.accessToken
            } else {
                response.close()
                clearSessionSync()
                return null
            }
        }
    }

    override fun notifySessionExpired() {
        clearSessionSync()
        _sessionState.value = SessionState.LoggedOut("Your session has expired. Please sign in again.")
    }

    suspend fun login(email: String, password: String):Result<Unit> = withContext(ioDispatcher) {
        try {
            val deviceId = deviceIdProvider.getDeviceId()
            val request = LoginRequest(email = email, password = password, deviceId = deviceId)
            val response = apiService.login(request)

            if (response.isSuccessful) {
                val tokenResponse = response.body() ?: throw IllegalStateException("Empty login response")
                val session = UserSession(
                    accessToken = tokenResponse.accessToken,
                    accessExpiresIn = tokenResponse.accessExpiresIn,
                    refreshToken = tokenResponse.refreshToken,
                    mustChangePassword = tokenResponse.mustChangePassword
                )
                saveSessionToDisk(session)
                currentSession = session

                if (tokenResponse.mustChangePassword) {
                    _sessionState.value = SessionState.MustChangePassword
                } else {
                    // Fetch profile for profile screen
                    val profile = fetchProfileInternal()
                    if (profile.mustChangePassword) {
                        saveSessionToDisk(session.copy(mustChangePassword = true, profile = profile))
                        _sessionState.value = SessionState.MustChangePassword
                    } else {
                        saveSessionToDisk(session.copy(profile = profile))
                        _sessionState.value = SessionState.Authenticated(profile)
                    }
                }
                Result.success(Unit)
            } else {
                val exception = parseErrorResponse(response)
                Result.failure(exception)
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val request = ChangePasswordRequest(currentPassword = currentPassword, newPassword = newPassword)
            val response = apiService.changePassword(request)

            if (response.isSuccessful) {
                val session = currentSession ?: loadSessionFromDisk()
                val updatedSession = session?.copy(mustChangePassword = false)
                if (updatedSession != null) {
                    saveSessionToDisk(updatedSession)
                    currentSession = updatedSession
                }

                val profile = fetchProfileInternal()
                val sessionWithProfile = (updatedSession ?: session)?.copy(
                    mustChangePassword = false,
                    profile = profile
                )
                if (sessionWithProfile != null) {
                    saveSessionToDisk(sessionWithProfile)
                    currentSession = sessionWithProfile
                }
                _sessionState.value = SessionState.Authenticated(profile)
                Result.success(Unit)
            } else {
                val exception = parseErrorResponse(response)
                Result.failure(exception)
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> = withContext(ioDispatcher) {
        val session = currentSession ?: loadSessionFromDisk()
        if (session != null) {
            try {
                apiService.logout(LogoutRequest(session.refreshToken))
            } catch (e: Exception) {
                // Even if remote logout fails, proceed to remove local session
            }
        }
        clearSessionSync()
        _sessionState.value = SessionState.LoggedOut()
        Result.success(Unit)
    }

    suspend fun refreshProfile(): Result<MemberProfileResponse> = withContext(ioDispatcher) {
        try {
            val profile = fetchProfileInternal()
            val session = currentSession ?: loadSessionFromDisk()
            if (session != null) {
                val updated = session.copy(profile = profile, mustChangePassword = profile.mustChangePassword)
                saveSessionToDisk(updated)
                currentSession = updated
            }
            if (profile.mustChangePassword) {
                _sessionState.value = SessionState.MustChangePassword
            } else {
                _sessionState.value = SessionState.Authenticated(profile)
            }
            Result.success(profile)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    private suspend fun refreshProfileSilently() {
        try {
            val profile = fetchProfileInternal()
            val session = currentSession
            if (session != null) {
                val updated = session.copy(profile = profile, mustChangePassword = profile.mustChangePassword)
                saveSessionToDisk(updated)
                currentSession = updated
            }
            if (profile.mustChangePassword) {
                _sessionState.value = SessionState.MustChangePassword
            } else {
                _sessionState.value = SessionState.Authenticated(profile)
            }
        } catch (e: Exception) {
            // Keep using cached profile
        }
    }

    private suspend fun fetchProfileInternal(): MemberProfileResponse {
        val response = apiService.getMe()
        if (response.isSuccessful) {
            return response.body() ?: throw IllegalStateException("Empty profile response")
        } else {
            throw parseErrorResponse(response)
        }
    }

    private fun <T> parseErrorResponse(response: Response<T>): ApiException {
        val retryAfterHeader = response.headers()["Retry-After"]?.toLongOrNull()
        val errorBody = response.errorBody()?.string()
        val errorResponse = if (!errorBody.isNullOrEmpty()) {
            try {
                json.decodeFromString<ApiErrorResponse>(errorBody)
            } catch (e: Exception) {
                ApiErrorResponse(status = response.code(), detail = errorBody)
            }
        } else {
            ApiErrorResponse(status = response.code(), detail = response.message())
        }
        return ApiException(errorResponse, retryAfterHeader)
    }

    private fun loadSessionFromDisk(): UserSession? {
        if (!sessionFile.exists()) return null
        return try {
            val encryptedBytes = sessionFile.readBytes()
            val decryptedBytes = crypto.decrypt(encryptedBytes)
            val jsonString = String(decryptedBytes, Charsets.UTF_8)
            json.decodeFromString<UserSession>(jsonString)
        } catch (e: Exception) {
            sessionFile.delete()
            null
        }
    }

    private fun saveSessionToDisk(session: UserSession) {
        saveSessionToDiskSync(session)
    }

    private fun saveSessionToDiskSync(session: UserSession) {
        try {
            val jsonString = json.encodeToString(session)
            val encryptedBytes = crypto.encrypt(jsonString.toByteArray(Charsets.UTF_8))
            sessionFile.writeBytes(encryptedBytes)
            currentSession = session
        } catch (e: Exception) {
            // Error writing session
        }
    }

    private fun clearSessionSync() {
        try {
            if (sessionFile.exists()) {
                sessionFile.delete()
            }
        } catch (e: Exception) {
            // Ignore
        }
        currentSession = null
    }
}
