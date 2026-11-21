package com.example.libraryaccount

import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

data class MemberProfile(
    val name: String,
    val email: String,
    val memberId: String,
    val memberSince: String
)

class ApiException(val statusCode: Int, message: String) : IOException(message)

/** Small REST client for the library service. The production API base URL is configured here. */
class LibraryApi {
    private val baseUrl = "https://api.library.example.com/v1/"

    fun signIn(email: String, password: String): String {
        val body = JSONObject().apply {
            put("email", email)
            put("password", password)
        }
        val response = request("auth/login", "POST", body.toString(), null)
        return response.optString("access_token").ifBlank {
            response.optString("token").ifBlank { throw IOException("The sign-in response did not include a token") }
        }
    }

    fun profile(token: String): MemberProfile {
        val json = request("me", "GET", null, token)
        val member = json.optJSONObject("member") ?: json
        return MemberProfile(
            name = member.optString("name").ifBlank { member.optString("full_name") },
            email = member.optString("email"),
            memberId = member.optString("id").ifBlank { member.optString("member_id") },
            memberSince = member.optString("member_since").ifBlank { member.optString("created_at") }
        )
    }

    private fun request(path: String, method: String, body: String?, token: String?): JSONObject {
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 15_000
            useCaches = false
            setRequestProperty("Accept", "application/json")
            token?.let { setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            body?.let { connection.outputStream.use { stream -> stream.write(it.toByteArray(StandardCharsets.UTF_8)) } }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = try { JSONObject(text).optString("message") } catch (_: JSONException) { "" }
                throw ApiException(status, message.ifBlank { "Request failed ($status)" })
            }
            return if (text.isBlank()) JSONObject() else JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }
}
