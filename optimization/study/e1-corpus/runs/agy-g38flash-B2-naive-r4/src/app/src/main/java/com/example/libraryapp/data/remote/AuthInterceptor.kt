package com.example.libraryapp.data.remote

import com.example.libraryapp.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor that appends the Bearer token to authenticated library requests.
 */
class AuthInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Bypass adding token for login endpoint
        if (originalRequest.url.encodedPath.contains("auth/login")) {
            return chain.proceed(originalRequest)
        }

        val token = sessionManager.getAuthToken()
        val authenticatedRequest = if (!token.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .build()
        } else {
            originalRequest.newBuilder()
                .header("Accept", "application/json")
                .build()
        }

        val response = chain.proceed(authenticatedRequest)

        // If server returns 401 Unauthorized, automatically clear invalid session
        if (response.code == 401) {
            sessionManager.clearSession()
        }

        return response
    }
}
