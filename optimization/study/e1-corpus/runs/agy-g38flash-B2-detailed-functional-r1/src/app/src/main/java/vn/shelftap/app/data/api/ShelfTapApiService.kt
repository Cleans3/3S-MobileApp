package vn.shelftap.app.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import vn.shelftap.app.data.model.ChangePasswordRequest
import vn.shelftap.app.data.model.LoginRequest
import vn.shelftap.app.data.model.LoginResponse
import vn.shelftap.app.data.model.LogoutRequest
import vn.shelftap.app.data.model.ProfileResponse
import vn.shelftap.app.data.model.RefreshRequest

interface ShelfTapApiService {

    @POST("api/auth/login")
    suspend fun login(
        @Body body: LoginRequest
    ): Response<LoginResponse>

    @POST("api/auth/refresh")
    suspend fun refresh(
        @Body body: RefreshRequest
    ): Response<LoginResponse>

    @POST("api/auth/logout")
    suspend fun logout(
        @Body body: LogoutRequest
    ): Response<Unit>

    @GET("api/me")
    suspend fun getProfile(): Response<ProfileResponse>

    @POST("api/me/password")
    suspend fun changePassword(
        @Body body: ChangePasswordRequest
    ): Response<Unit>
}
