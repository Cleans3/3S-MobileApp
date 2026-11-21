package vn.shelftap.app.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import vn.shelftap.app.data.api.models.ChangePasswordRequest
import vn.shelftap.app.data.api.models.LoginRequest
import vn.shelftap.app.data.api.models.LogoutRequest
import vn.shelftap.app.data.api.models.MemberProfileResponse
import vn.shelftap.app.data.api.models.RefreshRequest
import vn.shelftap.app.data.api.models.TokenResponse

interface ShelfTapApiService {

    @POST("/api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<TokenResponse>

    @POST("/api/auth/refresh")
    suspend fun refresh(
        @Body request: RefreshRequest
    ): Response<TokenResponse>

    @POST("/api/auth/logout")
    suspend fun logout(
        @Body request: LogoutRequest
    ): Response<Unit>

    @GET("/api/me")
    suspend fun getMe(): Response<MemberProfileResponse>

    @POST("/api/me/password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest
    ): Response<Unit>
}
