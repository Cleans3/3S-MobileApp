package com.library.member

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

data class Member(val name: String, val email: String, val memberId: String, val joined: String)
data class ApiResult<T>(val value: T? = null, val error: String? = null)

class LibraryApi(context: Context) {
    private val baseUrl = context.getString(com.library.member.R.string.api_base_url).trimEnd('/')
    private val executor = Executors.newSingleThreadExecutor()

    fun signIn(email: String, password: String, callback: (ApiResult<String>) -> Unit) {
        executor.execute {
            try {
                val body = JSONObject().put("email", email).put("password", password).toString()
                val response = request("/v1/auth/login", "POST", body, null)
                val json = JSONObject(response)
                val token = json.optString("token").ifBlank { json.optString("access_token") }
                if (token.isBlank()) callback(ApiResult(error = "The service returned no sign-in token."))
                else callback(ApiResult(value = token))
            } catch (e: Exception) { callback(ApiResult(error = message(e))) }
        }
    }

    fun member(token: String, callback: (ApiResult<Member>) -> Unit) {
        executor.execute {
            try {
                val json = JSONObject(request("/v1/members/me", "GET", null, token))
                val name = json.optString("name").ifBlank { listOf(json.optString("first_name"), json.optString("last_name")).filter { it.isNotBlank() }.joinToString(" ") }
                callback(ApiResult(value = Member(name.ifBlank { "Library member" }, json.optString("email"), json.optString("member_id").ifBlank { json.optString("id") }, json.optString("joined_at").ifBlank { json.optString("created_at") })))
            } catch (e: Exception) { callback(ApiResult(error = message(e))) }
        }
    }

    private fun request(path: String, method: String, body: String?, token: String?): String {
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method; connectTimeout = 15000; readTimeout = 15000
            setRequestProperty("Accept", "application/json")
            if (body != null) { doOutput = true; setRequestProperty("Content-Type", "application/json") }
            if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
        }
        body?.let { connection.outputStream.use { stream -> stream.write(it.toByteArray(Charsets.UTF_8)) } }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.let { BufferedReader(InputStreamReader(it)).use { reader -> reader.readText() } }.orEmpty()
        connection.disconnect()
        if (code !in 200..299) throw IllegalStateException(errorFrom(text, code))
        return text
    }

    private fun errorFrom(text: String, code: Int): String = try { JSONObject(text).optString("message").ifBlank { "Request failed ($code)." } } catch (_: Exception) { "Request failed ($code)." }
    private fun message(e: Exception) = e.message?.takeIf { it.isNotBlank() } ?: "Unable to reach the library service."
}
