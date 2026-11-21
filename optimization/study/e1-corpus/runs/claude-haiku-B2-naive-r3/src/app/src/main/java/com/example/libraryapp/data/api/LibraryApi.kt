package com.example.libraryapp.data.api

import com.example.libraryapp.data.model.LoginRequest
import com.example.libraryapp.data.model.LoginResponse
import com.example.libraryapp.data.model.MemberProfile
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** Retrofit description of the library REST API. Base URL comes from BuildConfig.API_BASE_URL. */
interface LibraryApi {

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/v1/auth/logout")
    suspend fun logout()

    @GET("api/v1/members/me")
    suspend fun getMyProfile(): MemberProfile
}
