package vn.shelftap.app.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import vn.shelftap.app.data.repository.AuthRepository

class AuthInterceptor(
    private val authRepository: AuthRepository
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = runBlocking {
            try {
                authRepository.accessToken()
            } catch (e: Exception) {
                ""
            }
        }

        val requestBuilder = originalRequest.newBuilder()
        if (token.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }
        requestBuilder.header("Accept", "application/json")

        return chain.proceed(requestBuilder.build())
    }
}
