package com.example.libcatalogue.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface BookApi {

    /**
     * Returns one page of the catalogue. Pages are 1-based.
     * [query] is null when the member has not typed a search.
     */
    @GET("books")
    suspend fun searchBooks(
        @Query("q") query: String?,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): BookPageDto
}
