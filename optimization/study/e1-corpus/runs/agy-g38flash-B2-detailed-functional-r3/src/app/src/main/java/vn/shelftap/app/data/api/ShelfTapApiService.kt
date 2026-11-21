package vn.shelftap.app.data.api

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import vn.shelftap.app.data.model.AuthResponse
import vn.shelftap.app.data.model.ChangePasswordRequest
import vn.shelftap.app.data.model.LoginRequest
import vn.shelftap.app.data.model.LogoutRequest
import vn.shelftap.app.data.model.MemberProfile
import vn.shelftap.app.data.model.RefreshRequest

interface ShelfTapApiService {

    @POST("api/auth/login")
    suspend fun login(
        @Body body: LoginRequest
    ): Response<AuthResponse>

    @POST("api/auth/refresh")
    fun refreshTokenSync(
        @Body body: RefreshRequest
    ): Call<AuthResponse>

    @POST("api/auth/refresh")
    suspend fun refresh(
        @Body body: RefreshRequest
    ): Response<AuthResponse>

    @POST("api/auth/logout")
    suspend fun logout(
        @Body body: LogoutRequest
    ): Response<Unit>

    @GET("api/me")
    suspend fun getMe(): Response<MemberProfile>

    @POST("api/me/password")
    suspend fun changePassword(
        @Body body: ChangePasswordRequest
    ): Response<Unit>
}
