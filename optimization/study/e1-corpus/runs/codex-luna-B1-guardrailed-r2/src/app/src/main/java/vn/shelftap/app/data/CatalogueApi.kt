package vn.shelftap.app.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class TitlePageDto(
    val items: List<TitleDto>,
    val page: Int,
    val size: Int,
    val total: Int
)

interface CatalogueApi {
    @GET("api/titles")
    suspend fun titles(@Query("q") query: String = "", @Query("page") page: Int, @Query("size") size: Int): TitlePageDto

    @GET("api/titles/{id}")
    suspend fun title(@Path("id") id: String): TitleDto
}
