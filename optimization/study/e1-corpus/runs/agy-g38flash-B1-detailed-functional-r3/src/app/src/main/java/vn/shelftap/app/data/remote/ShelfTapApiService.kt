package vn.shelftap.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import vn.shelftap.app.data.model.CatalogueResponse
import vn.shelftap.app.data.model.Title

interface ShelfTapApiService {

    @GET("/api/titles")
    suspend fun getTitles(
        @Query("q") query: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): CatalogueResponse

    @GET("/api/titles/{id}")
    suspend fun getTitle(
        @Path("id") id: String
    ): Title
}
