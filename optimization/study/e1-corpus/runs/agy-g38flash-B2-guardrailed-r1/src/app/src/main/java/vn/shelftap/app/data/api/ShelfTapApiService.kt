package vn.shelftap.app.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import vn.shelftap.app.data.models.AuthTokenResponse
import vn.shelftap.app.data.models.ChangePasswordRequest
import vn.shelftap.app.data.models.LoginRequest
import vn.shelftap.app.data.models.LogoutRequest
import vn.shelftap.app.data.models.MemberProfileResponse
import vn.shelftap.app.data.models.RefreshRequest

interface ShelfTapApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthTokenResponse>

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): Response<AuthTokenResponse>

    @POST("api/auth/logout")
    suspend fun logout(@Body request: LogoutRequest): Response<Unit>

    @GET("api/me")
    suspend fun getProfile(): Response<MemberProfileResponse>

    @POST("api/me/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Unit>
}
