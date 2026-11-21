package vn.shelftap.app.data.api

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import vn.shelftap.app.data.local.PreferenceManager
import vn.shelftap.app.data.model.ApiErrorResponse
import vn.shelftap.app.data.model.ApiException
import java.util.concurrent.TimeUnit

object ApiClient {

    const val BASE_URL = "https://api.shelftap.example/"

    val gson: Gson = Gson()

    fun createRefreshService(): ShelfTapApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ShelfTapApiService::class.java)
    }

    fun createApiService(
        preferenceManager: PreferenceManager,
        onSessionExpired: () -> Unit
    ): ShelfTapApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val refreshService = createRefreshService()

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(preferenceManager))
            .authenticator(AuthAuthenticator(preferenceManager, refreshService, onSessionExpired))
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ShelfTapApiService::class.java)
    }

    fun <T> parseError(response: Response<T>): ApiException {
        val errorBody = response.errorBody()?.string()
        val retryAfter = response.headers()["Retry-After"]?.toLongOrNull()

        val apiError = if (!errorBody.isNullOrBlank()) {
            try {
                gson.fromJson(errorBody, ApiErrorResponse::class.java)
            } catch (e: Exception) {
                ApiErrorResponse(
                    type = "about:blank",
                    title = "Network error",
                    status = response.code(),
                    detail = errorBody,
                    code = null
                )
            }
        } else {
            ApiErrorResponse(
                type = "about:blank",
                title = "HTTP ${response.code()}",
                status = response.code(),
                detail = response.message(),
                code = null
            )
        }

        return ApiException(
            error = apiError ?: ApiErrorResponse(status = response.code(), detail = "Unknown error"),
            httpCode = response.code(),
            retryAfterSeconds = retryAfter
        )
    }
}
