package com.library.app.data.api

import com.library.app.data.model.AuthResponse
import com.library.app.data.model.LoanItem
import com.library.app.data.model.MemberProfile
import com.library.app.data.model.SignInRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface LibraryApiService {

    @POST("api/v1/auth/signin")
    suspend fun signIn(
        @Body request: SignInRequest
    ): Response<AuthResponse>

    @GET("api/v1/member/profile")
    suspend fun getProfile(): Response<MemberProfile>

    @GET("api/v1/member/loans")
    suspend fun getLoans(): Response<List<LoanItem>>

    @POST("api/v1/auth/signout")
    suspend fun signOut(): Response<Unit>
}
