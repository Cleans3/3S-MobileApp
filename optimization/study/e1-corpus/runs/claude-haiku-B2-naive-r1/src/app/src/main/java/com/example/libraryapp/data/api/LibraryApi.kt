package com.example.libraryapp.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** Endpoints of the library REST API. Paths are relative to BuildConfig.LIBRARY_API_BASE_URL. */
interface LibraryApi {

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/v1/members/me")
    suspend fun currentMember(): MemberProfile
}
