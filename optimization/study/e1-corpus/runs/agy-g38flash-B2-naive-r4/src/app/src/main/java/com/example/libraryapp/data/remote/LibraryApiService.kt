package com.example.libraryapp.data.remote

import com.example.libraryapp.data.model.LoanItem
import com.example.libraryapp.data.model.LoginRequest
import com.example.libraryapp.data.model.LoginResponse
import com.example.libraryapp.data.model.MemberProfile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit REST API interface for Library authentication and member operations.
 */
interface LibraryApiService {

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("api/v1/members/me")
    suspend fun getMemberProfile(): Response<MemberProfile>

    @GET("api/v1/members/me/loans")
    suspend fun getActiveLoans(): Response<List<LoanItem>>
}
