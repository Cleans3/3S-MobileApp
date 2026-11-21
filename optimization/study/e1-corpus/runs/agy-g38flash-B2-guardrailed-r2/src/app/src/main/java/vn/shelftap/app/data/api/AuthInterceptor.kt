package vn.shelftap.app.data.api

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response
import vn.shelftap.app.data.api.models.ApiErrorCodes
import vn.shelftap.app.data.api.models.ApiErrorResponse

interface TokenProvider {
    fun getAccessToken(): String?
    fun refreshTokenSynchronized(failedToken: String?): String?
    fun notifySessionExpired()
}

class AuthInterceptor(
    private val tokenProvider: TokenProvider,
    private val json: Json
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        // Unauthenticated endpoints: login and refresh
        if (path.endsWith("/api/auth/login") || path.endsWith("/api/auth/refresh")) {
            return chain.proceed(originalRequest)
        }

        val token = tokenProvider.getAccessToken()
        val authRequest = if (!token.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        val response = chain.proceed(authRequest)

        // Handle 401 TOKEN_INVALID
        if (response.code == 401) {
            val isTokenInvalid = try {
                val bodyString = response.peekBody(65536).string()
                val error = json.decodeFromString<ApiErrorResponse>(bodyString)
                error.code == ApiErrorCodes.TOKEN_INVALID
            } catch (e: Exception) {
                false
            }

            if (isTokenInvalid) {
                val newToken = tokenProvider.refreshTokenSynchronized(token)
                if (newToken != null) {
                    response.close()
                    val retryRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer $newToken")
                        .build()
                    return chain.proceed(retryRequest)
                } else {
                    tokenProvider.notifySessionExpired()
                    return response
                }
            }
        }

        return response
    }
}
