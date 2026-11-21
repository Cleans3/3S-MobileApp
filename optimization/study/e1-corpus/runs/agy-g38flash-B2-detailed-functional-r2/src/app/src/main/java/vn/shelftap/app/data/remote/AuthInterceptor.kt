package vn.shelftap.app.data.remote

import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import vn.shelftap.app.data.local.SessionManager
import vn.shelftap.app.data.model.ApiError
import vn.shelftap.app.data.model.AuthResponse
import vn.shelftap.app.data.model.RefreshRequest

/**
 * OkHttp interceptor that attaches the Bearer accessToken to requests and handles
 * 401 TOKEN_INVALID responses by refreshing the token once and retrying the request.
 * If the refresh also fails, the session is cleared and the member is returned to sign-in.
 */
class AuthInterceptor(
    private val sessionManager: SessionManager,
    private val rawClientProvider: () -> OkHttpClient,
    private val baseUrl: String = "https://api.shelftap.example"
) : Interceptor {

    private val gson = Gson()
    private val refreshLock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        val isAuthEndpoint = path.contains("/api/auth/login") || path.contains("/api/auth/refresh")

        val token = sessionManager.getAccessToken()
        val authenticatedRequest = if (!token.isNullOrEmpty() && !isAuthEndpoint) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        var response = chain.proceed(authenticatedRequest)

        // When a call fails with 401 TOKEN_INVALID, refresh the token once and repeat the call
        if (response.code == 401 && !isAuthEndpoint) {
            val errorBody = response.peekBody(1024 * 64).string()
            val errorCode = try {
                gson.fromJson(errorBody, ApiError::class.java)?.code
            } catch (e: Exception) {
                null
            }

            if (errorCode == ErrorCodes.TOKEN_INVALID) {
                val refreshedAccessToken = synchronized(refreshLock) {
                    val currentToken = sessionManager.getAccessToken()
                    val usedToken = authenticatedRequest.header("Authorization")?.removePrefix("Bearer ")

                    // If another thread already refreshed token while this thread waited
                    if (currentToken != null && currentToken != usedToken) {
                        currentToken
                    } else {
                        performTokenRefresh()
                    }
                }

                if (refreshedAccessToken != null) {
                    // Retry the original request with the new access token
                    response.close()
                    val retryRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer $refreshedAccessToken")
                        .build()
                    response = chain.proceed(retryRequest)
                } else {
                    // If the refresh also fails, the member is returned to the sign-in screen
                    sessionManager.clearSession()
                }
            }
        }

        return response
    }

    private fun performTokenRefresh(): String? {
        val currentRefreshToken = sessionManager.getRefreshToken() ?: return null
        val deviceId = sessionManager.getDeviceId()

        val refreshRequestPayload = RefreshRequest(
            refreshToken = currentRefreshToken,
            deviceId = deviceId
        )
        val jsonPayload = gson.toJson(refreshRequestPayload)
        val requestBody = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/api/auth/refresh")
            .post(requestBody)
            .build()

        return try {
            val refreshResponse = rawClientProvider().newCall(request).execute()
            if (refreshResponse.isSuccessful) {
                val responseBody = refreshResponse.body?.string()
                if (!responseBody.isNullOrEmpty()) {
                    val authResponse = gson.fromJson(responseBody, AuthResponse::class.java)
                    // Save new single-use refresh token and access token
                    sessionManager.saveAuth(authResponse)
                    authResponse.accessToken
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
