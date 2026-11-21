package vn.shelftap.app.data.api

import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import vn.shelftap.app.data.local.DeviceIdProvider
import vn.shelftap.app.data.local.SessionManager
import vn.shelftap.app.data.model.ErrorResponse
import vn.shelftap.app.data.model.LoginResponse
import vn.shelftap.app.data.model.RefreshRequest

/**
 * Handles adding Bearer token to authenticated calls, peeking responses for 401 TOKEN_INVALID,
 * executing token refresh once with deviceId and single-use refresh token, and retrying the call.
 * If token refresh fails, expires the session so the member is directed to the sign-in screen.
 */
class AuthInterceptor(
    private val sessionManager: SessionManager,
    private val deviceIdProvider: DeviceIdProvider,
    private val baseUrl: String = "https://api.shelftap.example/"
) : Interceptor {

    private val gson = Gson()
    private val refreshClient = OkHttpClient.Builder().build()
    private val refreshLock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        // Skip auth header for public login/refresh endpoints
        val isAuthEndpoint = path.endsWith("/api/auth/login") || path.endsWith("/api/auth/refresh")

        val requestToExecute = if (!isAuthEndpoint) {
            val token = sessionManager.getAccessToken()
            if (!token.isNullOrBlank()) {
                originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                originalRequest
            }
        } else {
            originalRequest
        }

        var response = chain.proceed(requestToExecute)

        // When a call fails with 401 TOKEN_INVALID, refresh token once and repeat
        if (response.code == 401 && !isAuthEndpoint) {
            val peekBody = try {
                response.peekBody(65536).string()
            } catch (e: Exception) {
                null
            }

            val errorResponse = ErrorResponse.parse(peekBody)
            if (errorResponse?.code == ErrorResponse.CODE_TOKEN_INVALID) {
                synchronized(refreshLock) {
                    val currentAccessToken = sessionManager.getAccessToken()
                    val requestAuthHeader = requestToExecute.header("Authorization")
                    val originalTokenUsed = requestAuthHeader?.removePrefix("Bearer ")?.trim()

                    // If token was already refreshed by another concurrent thread, retry immediately
                    if (!currentAccessToken.isNullOrBlank() && currentAccessToken != originalTokenUsed) {
                        response.close()
                        val retriedRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer $currentAccessToken")
                            .build()
                        return chain.proceed(retriedRequest)
                    }

                    // Otherwise perform the synchronous refresh
                    val refreshToken = sessionManager.getRefreshToken()
                    if (refreshToken.isNullOrBlank()) {
                        sessionManager.notifySessionExpired()
                        return response
                    }

                    val refreshedSuccess = performRefresh(refreshToken)
                    if (refreshedSuccess) {
                        val newAccessToken = sessionManager.getAccessToken()
                        response.close()
                        val retriedRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer $newAccessToken")
                            .build()
                        return chain.proceed(retriedRequest)
                    } else {
                        // Refresh failed, return to sign-in screen
                        sessionManager.notifySessionExpired()
                    }
                }
            }
        }

        return response
    }

    private fun performRefresh(refreshToken: String): Boolean {
        return try {
            val deviceId = deviceIdProvider.getDeviceId()
            val requestBodyJson = gson.toJson(RefreshRequest(refreshToken = refreshToken, deviceId = deviceId))
            val refreshUrl = if (baseUrl.endsWith("/")) {
                baseUrl + "api/auth/refresh"
            } else {
                "$baseUrl/api/auth/refresh"
            }

            val refreshRequest = Request.Builder()
                .url(refreshUrl)
                .post(requestBodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val refreshResponse = refreshClient.newCall(refreshRequest).execute()
            if (refreshResponse.isSuccessful) {
                val bodyString = refreshResponse.body?.string()
                refreshResponse.close()
                if (!bodyString.isNullOrBlank()) {
                    val loginResponse = gson.fromJson(bodyString, LoginResponse::class.java)
                    if (loginResponse != null && !loginResponse.accessToken.isNullOrBlank()) {
                        sessionManager.updateTokens(loginResponse)
                        return true
                    }
                }
            } else {
                refreshResponse.close()
            }
            false
        } catch (e: Exception) {
            false
        }
    }
}
