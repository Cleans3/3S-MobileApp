package vn.shelftap.app.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

const val BASE_URL = "https://api.shelftap.example/"

interface ShelfTapApi {

    /** GET /api/titles. A null [q] is omitted from the request. */
    @GET("api/titles")
    suspend fun titles(
        @Query("q") q: String?,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): PageDto

    @GET("api/titles/{id}")
    suspend fun title(@Path("id") id: String): TitleDto
}

fun createShelfTapApi(): ShelfTapApi {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    return Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ShelfTapApi::class.java)
}
