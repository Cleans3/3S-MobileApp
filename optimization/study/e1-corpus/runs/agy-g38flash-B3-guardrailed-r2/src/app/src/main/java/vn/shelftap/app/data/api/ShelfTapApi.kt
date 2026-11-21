package vn.shelftap.app.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import vn.shelftap.app.data.model.HistoryItem
import vn.shelftap.app.data.model.Loan
import vn.shelftap.app.data.model.PrivacyRequest
import vn.shelftap.app.data.model.TitleDetails
import vn.shelftap.app.data.model.UserProfile

interface ShelfTapApi {

    @GET("/api/me/loans")
    suspend fun getLoans(): List<Loan>

    @GET("/api/me/history")
    suspend fun getHistory(): List<HistoryItem>

    @POST("/api/loans/{loanId}/renew")
    suspend fun renewLoan(
        @Path("loanId") loanId: String
    ): Response<Loan>

    @GET("/api/me")
    suspend fun getMe(): UserProfile

    @PUT("/api/me/privacy")
    suspend fun updatePrivacy(
        @Body request: PrivacyRequest
    ): UserProfile

    @GET("/api/titles/{titleId}")
    suspend fun getTitleDetails(
        @Path("titleId") titleId: String
    ): TitleDetails
}
