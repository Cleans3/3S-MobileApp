package vn.shelftap.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import vn.shelftap.app.data.model.HistoryItem
import vn.shelftap.app.data.model.Loan
import vn.shelftap.app.data.model.MemberProfile
import vn.shelftap.app.data.model.PrivacyRequest
import vn.shelftap.app.data.model.TitleDetail

/**
 * REST API definition for ShelfTap at https://api.shelftap.example
 */
interface ShelfTapApiService {

    @GET("/api/me/loans")
    suspend fun getLoans(): List<Loan>

    @GET("/api/me/history")
    suspend fun getHistory(): List<HistoryItem>

    @POST("/api/loans/{loanId}/renew")
    suspend fun renewLoan(
        @Path("loanId") loanId: String
    ): Response<Loan>

    @GET("/api/me")
    suspend fun getProfile(): MemberProfile

    @PUT("/api/me/privacy")
    suspend fun updatePrivacy(
        @Body request: PrivacyRequest
    ): MemberProfile

    @GET("/api/titles/{titleId}")
    suspend fun getTitleDetail(
        @Path("titleId") titleId: String
    ): TitleDetail
}
