package com.example.library.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * REST endpoints of the library service. Paths are relative to
 * BuildConfig.LIBRARY_API_BASE_URL. The bearer token is attached by AuthInterceptor.
 */
interface LibraryApi {

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/v1/members/me")
    suspend fun currentMember(): MemberProfile
}
