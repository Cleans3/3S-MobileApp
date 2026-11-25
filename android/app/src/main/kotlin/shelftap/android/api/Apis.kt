package shelftap.android.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Public endpoints: no bearer token, never retried through the token authenticator. */
interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest, @Header(Net.INTEGRITY_HEADER) integrity: String): TokenPair

    @POST("api/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest, @Header(Net.INTEGRITY_HEADER) integrity: String): TokenPair

    @POST("api/auth/logout")
    suspend fun logout(@Body body: LogoutRequest)

    @POST("api/cards/{cardId}/challenge")
    suspend fun challenge(@Path("cardId") cardId: String): ChallengeResponse

    @POST("api/cards/{cardId}/auth")
    suspend fun cardAuth(
        @Path("cardId") cardId: String,
        @Body body: CardAuthRequest,
        @Header(Net.INTEGRITY_HEADER) integrity: String,
    ): TokenPair
}

interface ShelfTapApi {
    @GET("api/titles")
    suspend fun titles(@Query("q") q: String, @Query("page") page: Int, @Query("size") size: Int): Page<TitleSummary>

    @GET("api/titles/{titleId}")
    suspend fun title(@Path("titleId") titleId: String): TitleDetail

    @GET("api/me")
    suspend fun me(): Me

    @POST("api/me/password")
    suspend fun changePassword(@Body body: ChangePasswordRequest)

    @PUT("api/me/privacy")
    suspend fun setPrivacy(@Body body: PrivacyRequest): Me

    @GET("api/me/fines")
    suspend fun fines(): List<Fine>

    @GET("api/me/loans")
    suspend fun loans(): List<Loan>

    @GET("api/me/history")
    suspend fun history(): List<HistoryItem>

    @GET("api/me/reservations")
    suspend fun reservations(): List<Reservation>

    @POST("api/loans")
    suspend fun checkout(@Body body: LoanRequest): Loan

    @POST("api/loans/{loanId}/renew")
    suspend fun renew(@Path("loanId") loanId: String): Loan

    @POST("api/reservations")
    suspend fun reserve(@Body body: ReservationRequest): Reservation

    @DELETE("api/reservations/{reservationId}")
    suspend fun cancelReservation(@Path("reservationId") reservationId: String)
}
