package vn.shelftap.app.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CatalogueApi {
    @GET("api/titles")
    suspend fun titles(@Query("q") query: String = "", @Query("page") page: Int, @Query("size") size: Int = 50): TitlesPage

    @GET("api/titles/{id}")
    suspend fun title(@Path("id") id: String): TitleDto
}
