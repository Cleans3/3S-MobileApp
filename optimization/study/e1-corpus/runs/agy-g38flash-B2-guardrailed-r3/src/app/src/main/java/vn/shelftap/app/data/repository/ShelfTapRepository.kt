package vn.shelftap.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import vn.shelftap.app.data.api.ShelfTapApiService
import vn.shelftap.app.data.model.ApiErrorResponse
import vn.shelftap.app.data.model.ApiResult
import vn.shelftap.app.data.model.AuthResponse
import vn.shelftap.app.data.model.ChangePasswordRequest
import vn.shelftap.app.data.model.LoginRequest
import vn.shelftap.app.data.model.LogoutRequest
import vn.shelftap.app.data.model.MemberProfile
import vn.shelftap.app.data.model.RefreshRequest
import vn.shelftap.app.data.storage.EncryptedSessionStorage
import java.util.concurrent.TimeUnit

class ShelfTapRepository(
    private val sessionStorage: EncryptedSessionStorage,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    },
    customApiService: ShelfTapApiService? = null
) {
    companion object {
        const val BASE_URL = "https://api.shelftap.example/"
        const val CODE_INVALID_CREDENTIALS = "INVALID_CREDENTIALS"
        const val CODE_ACCOUNT_LOCKED = "ACCOUNT_LOCKED"
        const val CODE_RATE_LIMITED = "RATE_LIMITED"
        const val CODE_TOKEN_INVALID = "TOKEN_INVALID"
        const val CODE_PASSWORD_CHANGE_REQUIRED = "PASSWORD_CHANGE_REQUIRED"
    }

    private val refreshMutex = Mutex()

    private val apiService: ShelfTapApiService = customApiService ?: run {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        retrofit.create(ShelfTapApiService::class.java)
    }

    private fun <T> parseError(response: Response<T>): ApiResult.Error {
        val errorBodyStr = try {
            response.errorBody()?.string()
        } catch (e: Exception) {
            null
        }
        val errorResponse: ApiErrorResponse? = errorBodyStr?.let {
            try {
                json.decodeFromString(ApiErrorResponse.serializer(), it)
            } catch (e: Exception) {
                null
            }
        }
        val retryAfter = response.headers()["Retry-After"]?.toIntOrNull()
        return ApiResult.Error(
            code = errorResponse?.code,
            statusCode = response.code(),
            message = errorResponse?.detail ?: errorResponse?.title,
            retryAfterSeconds = retryAfter
        )
    }

    suspend fun hasSession(): Boolean = withContext(ioDispatcher) {
        sessionStorage.hasSession()
    }

    suspend fun isMustChangePassword(): Boolean = withContext(ioDispatcher) {
        sessionStorage.isMustChangePassword()
    }

    suspend fun getCachedProfile(): MemberProfile? = withContext(ioDispatcher) {
        sessionStorage.getProfile()
    }

    suspend fun login(email: String, password: String): ApiResult<AuthResponse> = withContext(ioDispatcher) {
        val deviceId = sessionStorage.getOrCreateDeviceId()
        try {
            val response = apiService.login(
                LoginRequest(email = email, password = password, deviceId = deviceId)
            )
            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                sessionStorage.saveSession(
                    accessToken = auth.accessToken,
                    refreshToken = auth.refreshToken,
                    mustChangePassword = auth.mustChangePassword
                )
                return@withContext ApiResult.Success(auth)
            }
            return@withContext parseError(response)
        } catch (e: Exception) {
            return@withContext ApiResult.NetworkError(e)
        }
    }

    suspend fun refreshSession(): ApiResult<AuthResponse> = withContext(ioDispatcher) {
        refreshMutex.withLock {
            val refreshToken = sessionStorage.getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                sessionStorage.clearSession()
                return@withContext ApiResult.Error(
                    code = CODE_TOKEN_INVALID,
                    statusCode = 401,
                    message = "No refresh token available"
                )
            }

            val deviceId = sessionStorage.getOrCreateDeviceId()
            try {
                val response = apiService.refresh(
                    RefreshRequest(refreshToken = refreshToken, deviceId = deviceId)
                )
                if (response.isSuccessful && response.body() != null) {
                    val auth = response.body()!!
                    sessionStorage.saveSession(
                        accessToken = auth.accessToken,
                        refreshToken = auth.refreshToken,
                        mustChangePassword = auth.mustChangePassword
                    )
                    return@withContext ApiResult.Success(auth)
                }

                val error = parseError(response)
                sessionStorage.clearSession()
                return@withContext error
            } catch (e: Exception) {
                return@withContext ApiResult.NetworkError(e)
            }
        }
    }

    suspend fun getProfile(): ApiResult<MemberProfile> = withContext(ioDispatcher) {
        val accessToken = sessionStorage.getAccessToken()
        if (accessToken.isNullOrBlank()) {
            sessionStorage.clearSession()
            return@withContext ApiResult.Error(
                code = CODE_TOKEN_INVALID,
                statusCode = 401,
                message = "Not authenticated"
            )
        }

        try {
            val response = apiService.getProfile("Bearer $accessToken")
            if (response.isSuccessful && response.body() != null) {
                val profile = response.body()!!
                sessionStorage.saveProfile(profile)
                return@withContext ApiResult.Success(profile)
            }

            val error = parseError(response)
            if (error.code == CODE_TOKEN_INVALID || error.statusCode == 401) {
                val refreshResult = refreshSession()
                if (refreshResult is ApiResult.Success) {
                    val newAccessToken = sessionStorage.getAccessToken()
                    val retryResponse = apiService.getProfile("Bearer $newAccessToken")
                    if (retryResponse.isSuccessful && retryResponse.body() != null) {
                        val profile = retryResponse.body()!!
                        sessionStorage.saveProfile(profile)
                        return@withContext ApiResult.Success(profile)
                    }
                    val retryError = parseError(retryResponse)
                    if (retryError.code == CODE_TOKEN_INVALID || retryError.statusCode == 401) {
                        sessionStorage.clearSession()
                    }
                    return@withContext retryError
                } else {
                    sessionStorage.clearSession()
                    return@withContext refreshResult
                }
            }
            return@withContext error
        } catch (e: Exception) {
            return@withContext ApiResult.NetworkError(e)
        }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<Unit> =
        withContext(ioDispatcher) {
            val accessToken = sessionStorage.getAccessToken()
            if (accessToken.isNullOrBlank()) {
                sessionStorage.clearSession()
                return@withContext ApiResult.Error(
                    code = CODE_TOKEN_INVALID,
                    statusCode = 401,
                    message = "Not authenticated"
                )
            }

            try {
                val response = apiService.changePassword(
                    "Bearer $accessToken",
                    ChangePasswordRequest(currentPassword = currentPassword, newPassword = newPassword)
                )
                if (response.isSuccessful) {
                    sessionStorage.setMustChangePassword(false)
                    return@withContext ApiResult.Success(Unit)
                }

                val error = parseError(response)
                if (error.code == CODE_TOKEN_INVALID || error.statusCode == 401) {
                    val refreshResult = refreshSession()
                    if (refreshResult is ApiResult.Success) {
                        val newAccessToken = sessionStorage.getAccessToken()
                        val retryResponse = apiService.changePassword(
                            "Bearer $newAccessToken",
                            ChangePasswordRequest(currentPassword = currentPassword, newPassword = newPassword)
                        )
                        if (retryResponse.isSuccessful) {
                            sessionStorage.setMustChangePassword(false)
                            return@withContext ApiResult.Success(Unit)
                        }
                        val retryError = parseError(retryResponse)
                        if (retryError.code == CODE_TOKEN_INVALID || retryError.statusCode == 401) {
                            sessionStorage.clearSession()
                        }
                        return@withContext retryError
                    } else {
                        sessionStorage.clearSession()
                        return@withContext refreshResult
                    }
                }
                return@withContext error
            } catch (e: Exception) {
                return@withContext ApiResult.NetworkError(e)
            }
        }

    suspend fun logout(): ApiResult<Unit> = withContext(ioDispatcher) {
        val refreshToken = sessionStorage.getRefreshToken()
        try {
            if (!refreshToken.isNullOrBlank()) {
                apiService.logout(LogoutRequest(refreshToken = refreshToken))
            }
        } catch (_: Exception) {
            // Server notification best effort
        } finally {
            sessionStorage.clearSession()
        }
        ApiResult.Success(Unit)
    }
}
