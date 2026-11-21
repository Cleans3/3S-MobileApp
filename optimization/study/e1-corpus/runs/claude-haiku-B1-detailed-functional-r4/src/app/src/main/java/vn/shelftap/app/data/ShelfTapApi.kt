package vn.shelftap.app.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ShelfTapApi {

    /**
     * Pages through the catalogue. Leaving [q] null returns all titles, which is what the
     * offline sync uses. Search itself runs against the local database.
     */
    @GET("api/titles")
    suspend fun listTitles(
        @Query("q") q: String? = null,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): TitlePageDto

    @GET("api/titles/{id}")
    suspend fun getTitle(@Path("id") id: String): TitleDto

    companion object {
        const val BASE_URL = "https://api.shelftap.example/"
    }
}
