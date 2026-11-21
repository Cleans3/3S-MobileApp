package vn.shelftap.lab

import okhttp3.OkHttpClient
import okhttp3.Request

/** Fetches the book catalog from the ShelfTap API. */
class CatalogClient(private val baseUrl: String = BuildConfig.API_BASE_URL) {
    private val http = OkHttpClient()

    /** Blocking call: run it off the main thread. */
    fun fetchCatalog(): String {
        val request = Request.Builder().url(baseUrl + "api/books").build()
        http.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "HTTP " + response.code }
            return response.body.string()
        }
    }
}
