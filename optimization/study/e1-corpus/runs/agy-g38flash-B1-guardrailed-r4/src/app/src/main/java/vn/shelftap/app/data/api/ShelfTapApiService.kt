package vn.shelftap.app.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ShelfTapApiService {
    @GET("api/titles")
    suspend fun getTitles(
        @Query("q") query: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): TitlesResponseDto

    @GET("api/titles/{id}")
    suspend fun getTitleById(
        @Path("id") id: String
    ): TitleDto
}
