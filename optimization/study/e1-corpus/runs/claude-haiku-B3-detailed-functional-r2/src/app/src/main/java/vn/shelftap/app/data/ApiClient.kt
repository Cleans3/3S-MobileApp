package vn.shelftap.app.data

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import vn.shelftap.app.auth.AuthRepository

const val API_BASE_URL = "https://api.shelftap.example"

/** A non-2xx response from the ShelfTap API. */
class ApiException(
    val status: Int,
    val code: String?,
    message: String,
) : Exception(message)

class ApiClient(
    private val auth: AuthRepository,
    val json: Json,
) {
    /**
     * Sends one request with the bearer token and returns the response body.
     * Throws [ApiException] for non-2xx responses and IOException when the network fails.
     */
    suspend fun request(method: String, path: String, body: String? = null): String =
        withContext(Dispatchers.IO) {
            val token = auth.accessToken()
            val connection = URL(API_BASE_URL + path).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = method
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $token")
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (status !in 200..299) {
                    val error = runCatching { json.decodeFromString<ApiErrorBody>(text) }.getOrNull()
                    throw ApiException(
                        status = status,
                        code = error?.code,
                        message = error?.message ?: "Request failed with HTTP $status",
                    )
                }
                text
            } finally {
                connection.disconnect()
            }
        }

    suspend inline fun <reified T> getJson(path: String): T =
        json.decodeFromString<T>(request("GET", path))

    suspend inline fun <reified T> postJson(path: String, body: String = "{}"): T =
        json.decodeFromString<T>(request("POST", path, body))

    suspend inline fun <reified T> putJson(path: String, body: String): T =
        json.decodeFromString<T>(request("PUT", path, body))
}
