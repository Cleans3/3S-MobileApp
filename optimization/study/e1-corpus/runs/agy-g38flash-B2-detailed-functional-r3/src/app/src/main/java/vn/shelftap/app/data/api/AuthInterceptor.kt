package vn.shelftap.app.data.api

import okhttp3.Interceptor
import okhttp3.Response
import vn.shelftap.app.data.local.PreferenceManager

/**
 * Interceptor that attaches the Bearer token to authenticated calls.
 */
class AuthInterceptor(
    private val preferenceManager: PreferenceManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip adding auth header for login and refresh endpoints if already unauthenticated
        val path = originalRequest.url.encodedPath
        if (path.endsWith("api/auth/login") || path.endsWith("api/auth/refresh")) {
            return chain.proceed(originalRequest)
        }

        val token = preferenceManager.getAccessToken()
        return if (!token.isNullOrBlank()) {
            val authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(authenticatedRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}
