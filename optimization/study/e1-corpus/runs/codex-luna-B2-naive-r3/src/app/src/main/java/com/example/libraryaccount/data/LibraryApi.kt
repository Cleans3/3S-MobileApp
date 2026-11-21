package com.example.libraryaccount.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class MemberProfile(
    val name: String,
    val email: String,
    val memberId: String,
    val membership: String,
    val joinedDate: String
)

sealed class ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>()
    data class Failure(val message: String, val unauthorized: Boolean = false) : ApiResult<Nothing>()
}

/** Small REST client kept independent of the UI so the API base URL can be replaced per deployment. */
class LibraryApi(private val baseUrl: String) {
    suspend fun signIn(email: String, password: String): ApiResult<String> = withContext(Dispatchers.IO) {
        request("POST", "/auth/sign-in", "{\"email\":\"${email.jsonEscape()}\",\"password\":\"${password.jsonEscape()}\"}") { body ->
            JSONObject(body).optString("token").takeIf { it.isNotBlank() }
                ?: throw ApiException("The server did not return a session token.")
        }
    }

    suspend fun profile(token: String): ApiResult<MemberProfile> = withContext(Dispatchers.IO) {
        request("GET", "/members/me", null, token) { body ->
            val json = JSONObject(body)
            MemberProfile(
                name = json.optString("name", "Library member"),
                email = json.optString("email"),
                memberId = json.optString("memberId", json.optString("id", "—")),
                membership = json.optString("membership", "Active"),
                joinedDate = json.optString("joinedDate", "—")
            )
        }
    }

    private fun <T> request(
        method: String,
        path: String,
        body: String?,
        token: String? = null,
        decode: (String) -> T
    ): ApiResult<T> {
        val connection = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        return try {
            if (body != null) connection.outputStream.use { it.write(body.toByteArray()) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.let { BufferedReader(InputStreamReader(it)).use { reader -> reader.readText() } } ?: ""
            if (status in 200..299) ApiResult.Success(decode(response))
            else ApiResult.Failure(errorMessage(response, status), status == 401)
        } catch (error: Exception) {
            ApiResult.Failure(error.message ?: "Unable to reach the library service.")
        } finally {
            connection.disconnect()
        }
    }

    private fun errorMessage(body: String, status: Int): String =
        runCatching { JSONObject(body).optString("message") }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: if (status == 401) "Your email or password is incorrect." else "The library service is unavailable."

    private fun String.jsonEscape(): String = replace("\\", "\\\\").replace("\"", "\\\"")
    private class ApiException(message: String) : Exception(message)
}
