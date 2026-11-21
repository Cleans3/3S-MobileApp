package com.example.libcatalog.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Assumed REST contract of the library catalogue server:
 *   GET books?q={text}&page={n}&size={m}  ->  JSON array of books
 * Pages are 1-based. An array shorter than `size` means there are no more pages.
 * `q` is omitted when the member has not typed a search.
 */
interface BookApi {

    @GET("books")
    suspend fun searchBooks(
        @Query("q") query: String?,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): List<Book>
}

fun createBookApi(baseUrl: String, logging: Boolean): BookApi {
    val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .apply {
            if (logging) {
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .build()

    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(BookApi::class.java)
}
