package vn.shelftap.app.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ShelfTapApi {
    @POST("api/auth/login") suspend fun login(@Body body: LoginRequest): Response<AuthResponse>
    @POST("api/auth/refresh") suspend fun refresh(@Body body: RefreshRequest): Response<AuthResponse>
    @POST("api/auth/logout") suspend fun logout(@Body body: LogoutRequest): Response<Void>
    @GET("api/me") suspend fun me(): Response<Member>
    @POST("api/me/password") suspend fun changePassword(@Body body: PasswordRequest): Response<Void>
}
