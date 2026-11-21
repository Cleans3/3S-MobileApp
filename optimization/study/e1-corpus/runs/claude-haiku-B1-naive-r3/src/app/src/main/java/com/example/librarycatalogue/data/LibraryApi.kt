package com.example.librarycatalogue.data

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Library catalogue REST API.
 *
 * GET books?q=<text>&page=<n>&per_page=<n>  -> JSON array of books.
 * A null [query] omits the parameter and returns the whole catalogue.
 */
internal interface LibraryApi {
    @GET("books")
    suspend fun searchBooks(
        @Query("q") query: String?,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): List<BookDto>
}
