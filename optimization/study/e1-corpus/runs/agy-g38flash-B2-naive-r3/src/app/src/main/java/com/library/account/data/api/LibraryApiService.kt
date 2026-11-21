package com.library.account.data.api

import com.library.account.data.model.LoginRequest
import com.library.account.data.model.LoginResponse
import com.library.account.data.model.MemberProfile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit REST API interface for Library Member authentication and account management.
 */
interface LibraryApiService {

    /**
     * Authenticates a library member using their email and password.
     */
    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    /**
     * Retrieves the profile and account details for the authenticated member.
     * The auth token is automatically added via AuthInterceptor.
     */
    @GET("api/v1/members/me")
    suspend fun getProfile(): Response<MemberProfile>

    /**
     * Informs the server of the member's sign out.
     */
    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Unit>
}
