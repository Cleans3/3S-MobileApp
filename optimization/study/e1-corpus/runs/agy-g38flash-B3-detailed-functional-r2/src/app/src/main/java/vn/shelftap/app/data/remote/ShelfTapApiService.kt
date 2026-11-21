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
import vn.shelftap.app.data.model.PrivacyUpdateRequest
import vn.shelftap.app.data.model.TitleDetail

interface ShelfTapApiService {

    @GET("/api/me/loans")
    suspend fun getLoans(): Response<List<Loan>>

    @GET("/api/me/history")
    suspend fun getHistory(): Response<List<HistoryItem>>

    @POST("/api/loans/{loanId}/renew")
    suspend fun renewLoan(@Path("loanId") loanId: String): Response<Loan>

    @GET("/api/me")
    suspend fun getMemberProfile(): Response<MemberProfile>

    @PUT("/api/me/privacy")
    suspend fun updatePrivacy(@Body request: PrivacyUpdateRequest): Response<MemberProfile>

    @GET("/api/titles/{titleId}")
    suspend fun getTitleDetail(@Path("titleId") titleId: String): Response<TitleDetail>
}
