package vn.shelftap.app.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import vn.shelftap.app.data.model.BookDetail
import vn.shelftap.app.data.model.HistoryItem
import vn.shelftap.app.data.model.Loan
import vn.shelftap.app.data.model.MemberProfile
import vn.shelftap.app.data.model.PrivacyRequest

interface ShelfTapApiService {

    @GET("api/me/loans")
    suspend fun getLoans(): List<Loan>

    @GET("api/me/history")
    suspend fun getHistory(): List<HistoryItem>

    @POST("api/loans/{loanId}/renew")
    suspend fun renewLoan(
        @Path("loanId") loanId: String
    ): Loan

    @GET("api/me")
    suspend fun getProfile(): MemberProfile

    @PUT("api/me/privacy")
    suspend fun updatePrivacy(
        @Body request: PrivacyRequest
    ): MemberProfile

    @GET("api/titles/{titleId}")
    suspend fun getTitle(
        @Path("titleId") titleId: String
    ): BookDetail
}
