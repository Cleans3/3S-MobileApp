package vn.shelftap.app.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Header
import vn.shelftap.app.model.*

interface ShelfTapApi {
    @GET("api/me/loans") suspend fun loans(@Header("Authorization") authorization: String): List<Loan>
    @GET("api/me/history") suspend fun history(@Header("Authorization") authorization: String): List<HistoryItem>
    @POST("api/loans/{loanId}/renew") suspend fun renew(@Header("Authorization") authorization: String, @Path("loanId") loanId: String): Loan
    @GET("api/me") suspend fun member(@Header("Authorization") authorization: String): Member
    @PUT("api/me/privacy") suspend fun updatePrivacy(@Header("Authorization") authorization: String, @Body body: PrivacyRequest): Member
    @GET("api/titles/{titleId}") suspend fun title(@Header("Authorization") authorization: String, @Path("titleId") titleId: String): TitleDetail
}

data class PrivacyRequest(val historyEnabled: Boolean)
