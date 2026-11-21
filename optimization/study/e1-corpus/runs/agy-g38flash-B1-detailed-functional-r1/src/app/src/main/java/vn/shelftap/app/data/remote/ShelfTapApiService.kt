package vn.shelftap.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import vn.shelftap.app.data.model.Title
import vn.shelftap.app.data.model.TitleListResponse

/**
 * ShelfTap Catalogue REST API service.
 * Base URL: https://api.shelftap.example
 */
interface ShelfTapApiService {

    /**
     * Query catalogue titles with pagination and optional search term.
     *
     * @param query Search query matching title, author, ISBN or subject. Null or empty returns all titles.
     * @param page Zero-based page index.
     * @param size Page size (maximum 50).
     */
    @GET("/api/titles")
    suspend fun getTitles(
        @Query("q") query: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): TitleListResponse

    /**
     * Fetch a single title by its ID.
     *
     * @param id The title unique identifier.
     */
    @GET("/api/titles/{id}")
    suspend fun getTitleById(
        @Path("id") id: String
    ): Title
}
