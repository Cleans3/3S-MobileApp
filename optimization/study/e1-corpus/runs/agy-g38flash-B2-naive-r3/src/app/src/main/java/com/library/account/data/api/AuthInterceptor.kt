package com.library.account.data.api

import com.library.account.data.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp Interceptor that attaches the Bearer token to authenticated library requests.
 */
class AuthInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")

        val token = sessionManager.getAuthToken()
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val response = chain.proceed(requestBuilder.build())

        // If the token has expired or is unauthorized, handle session invalidation
        if (response.code == 401) {
            sessionManager.clearSession()
        }

        return response
    }
}
