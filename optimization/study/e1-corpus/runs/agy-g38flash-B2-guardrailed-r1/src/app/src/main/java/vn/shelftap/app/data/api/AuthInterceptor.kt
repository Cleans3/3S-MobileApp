package vn.shelftap.app.data.api

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import vn.shelftap.app.data.local.EncryptedSessionStorage
import vn.shelftap.app.data.models.ApiErrorResponse
import vn.shelftap.app.data.models.AuthTokenResponse
import vn.shelftap.app.data.models.RefreshRequest
import java.io.IOException

class AuthInterceptor(
    private val sessionStorage: EncryptedSessionStorage,
    private val json: Json,
    private val onSessionExpired: () -> Unit
) : Interceptor {

    companion object {
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val HEADER_RETRY_ATTEMPT = "X-ShelfTap-Retry"
        private const val BASE_URL = "https://api.shelftap.example/"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val refreshClient by lazy {
        OkHttpClient.Builder().build()
    }

    private val refreshLock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Attach Authorization header if access token exists and not already provided
        val accessToken = sessionStorage.getAccessTokenSync()
        val requestWithAuth = if (!accessToken.isNullOrBlank() && originalRequest.header(HEADER_AUTHORIZATION) == null) {
            originalRequest.newBuilder()
                .header(HEADER_AUTHORIZATION, "Bearer $accessToken")
                .build()
        } else {
            originalRequest
        }

        val response = chain.proceed(requestWithAuth)

        // Handle 401 TOKEN_INVALID with single refresh retry
        if (response.code == 401 && originalRequest.header(HEADER_RETRY_ATTEMPT) == null) {
            val isTokenInvalid = isTokenInvalidError(response)
            if (isTokenInvalid) {
                synchronized(refreshLock) {
                    // Check if token was refreshed by another thread while waiting
                    val latestAccessToken = sessionStorage.getAccessTokenSync()
                    if (latestAccessToken != null && latestAccessToken != accessToken) {
                        response.close()
                        val retriedRequest = originalRequest.newBuilder()
                            .header(HEADER_AUTHORIZATION, "Bearer $latestAccessToken")
                            .header(HEADER_RETRY_ATTEMPT, "1")
                            .build()
                        return chain.proceed(retriedRequest)
                    }

                    // Attempt synchronous token refresh
                    val currentRefreshToken = sessionStorage.getRefreshTokenSync()
                    val deviceId = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                        sessionStorage.getOrCreateDeviceId()
                    }

                    if (!currentRefreshToken.isNullOrBlank()) {
                        val refreshedTokens = executeRefreshToken(currentRefreshToken, deviceId)
                        if (refreshedTokens != null) {
                            kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                                sessionStorage.saveAuthTokens(
                                    accessToken = refreshedTokens.accessToken,
                                    refreshToken = refreshedTokens.refreshToken,
                                    mustChangePassword = refreshedTokens.mustChangePassword
                                )
                            }
                            response.close()
                            val retriedRequest = originalRequest.newBuilder()
                                .header(HEADER_AUTHORIZATION, "Bearer ${refreshedTokens.accessToken}")
                                .header(HEADER_RETRY_ATTEMPT, "1")
                                .build()
                            return chain.proceed(retriedRequest)
                        } else {
                            // Refresh also failed: notify session expired
                            onSessionExpired()
                        }
                    } else {
                        onSessionExpired()
                    }
                }
            }
        }

        return response
    }

    private fun isTokenInvalidError(response: Response): Boolean {
        return try {
            val peeked = response.peekBody(64 * 1024).string()
            val errorResponse = json.decodeFromString<ApiErrorResponse>(peeked)
            errorResponse.code == "TOKEN_INVALID"
        } catch (_: Exception) {
            false
        }
    }

    private fun executeRefreshToken(refreshToken: String, deviceId: String): AuthTokenResponse? {
        return try {
            val refreshPayload = json.encodeToString(
                RefreshRequest.serializer(),
                RefreshRequest(refreshToken = refreshToken, deviceId = deviceId)
            )
            val refreshRequest = Request.Builder()
                .url("${BASE_URL}api/auth/refresh")
                .post(refreshPayload.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val refreshResponse = refreshClient.newCall(refreshRequest).execute()
            if (refreshResponse.isSuccessful) {
                val bodyString = refreshResponse.body?.string() ?: return null
                json.decodeFromString<AuthTokenResponse>(bodyString)
            } else {
                null
            }
        } catch (_: IOException) {
            null
        } catch (_: Exception) {
            null
        }
    }
}
