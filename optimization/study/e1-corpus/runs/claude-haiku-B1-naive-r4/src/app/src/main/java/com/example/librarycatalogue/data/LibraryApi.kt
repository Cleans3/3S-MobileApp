package com.example.librarycatalogue.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Library catalogue REST API.
 *
 * Assumed contract (adapt to the real library system):
 *   GET books?page=<1-based>&per_page=<n>&q=<search text, optional>
 *   -> { "items": [BookDto...], "page": n, "total_pages": n }
 *
 * Searching is done server-side so the app never downloads the whole catalogue (~100k books).
 */
interface LibraryApi {

    @GET("books")
    suspend fun searchBooks(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
        @Query("q") query: String? = null,
    ): BookPageDto
}

@Serializable
data class BookPageDto(
    val items: List<BookDto> = emptyList(),
    val page: Int = 1,
    @SerialName("total_pages") val totalPages: Int = 1,
)

@Serializable
data class BookDto(
    val id: String,
    val title: String,
    val author: String = "",
    val year: Int? = null,
    val isbn: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
)
