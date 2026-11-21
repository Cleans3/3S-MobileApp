package com.example.library.data.api

import com.example.library.data.model.LoginRequest
import com.example.library.data.model.LoginResponse
import com.example.library.data.model.MemberProfile
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** Endpoints of the library REST API. See README.md for the contract. */
interface LibraryApi {

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/v1/auth/logout")
    suspend fun logout()

    @GET("api/v1/members/me")
    suspend fun getMyProfile(): MemberProfile
}
