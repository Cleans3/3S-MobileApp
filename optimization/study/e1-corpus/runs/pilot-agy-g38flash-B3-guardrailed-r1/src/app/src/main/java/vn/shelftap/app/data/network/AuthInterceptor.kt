package vn.shelftap.app.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import vn.shelftap.app.data.repository.AuthRepository

class AuthInterceptor(
    private val authRepository: AuthRepository
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = runBlocking(Dispatchers.IO) {
            authRepository.accessToken()
        }

        val authenticatedRequest = if (token.isNotBlank()) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .build()
        } else {
            request.newBuilder()
                .header("Accept", "application/json")
                .build()
        }

        return chain.proceed(authenticatedRequest)
    }
}
