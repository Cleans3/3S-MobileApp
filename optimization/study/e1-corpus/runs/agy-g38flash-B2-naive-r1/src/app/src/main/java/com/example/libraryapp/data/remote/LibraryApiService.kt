package com.example.libraryapp.data.remote

import com.example.libraryapp.data.model.AuthResponse
import com.example.libraryapp.data.model.LoginRequest
import com.example.libraryapp.data.model.LogoutResponse
import com.example.libraryapp.data.model.MemberProfileResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface LibraryApiService {

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<LogoutResponse>

    @GET("api/v1/members/me")
    suspend fun getMemberProfile(): Response<MemberProfileResponse>
}
