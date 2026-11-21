package vn.shelftap.app

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ApiClient {
    private val baseUrl = "https://api.shelftap.example"

    suspend fun login(email: String, password: String, deviceId: String): Session =
        parseSession(request("POST", "/api/auth/login", jsonOf("email" to email, "password" to password, "deviceId" to deviceId)))

    suspend fun refresh(refreshToken: String, deviceId: String): Session =
        parseSession(request("POST", "/api/auth/refresh", jsonOf("refreshToken" to refreshToken, "deviceId" to deviceId)))

    suspend fun logout(refreshToken: String) {
        request("POST", "/api/auth/logout", jsonOf("refreshToken" to refreshToken))
    }

    suspend fun profile(accessToken: String): MemberProfile {
        val json = JSONObject(request("GET", "/api/me", token = accessToken))
        MemberProfile(
            json.getString("memberId"), json.getString("fullName"), json.getString("email"),
            json.getString("memberType"), json.getString("status"), json.optBoolean("historyEnabled"),
            json.optBoolean("mustChangePassword")
        )
    }

    suspend fun changePassword(accessToken: String, current: String, new: String) {
        request("POST", "/api/me/password", jsonOf("currentPassword" to current, "newPassword" to new), accessToken)
    }

    private fun parseSession(body: String): Session {
        val json = JSONObject(body)
        return Session(json.getString("accessToken"), json.getString("refreshToken"), json.optLong("accessExpiresIn"), json.optBoolean("mustChangePassword"))
    }

    private fun jsonOf(vararg values: Pair<String, String>) = JSONObject().apply { values.forEach { put(it.first, it.second) } }.toString()

    private fun request(method: String, path: String, body: String? = null, token: String? = null): String {
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val error = runCatching { JSONObject(response) }.getOrNull()
                val retry = connection.getHeaderField("Retry-After")?.toIntOrNull()
                throw ApiException(error?.optString("code")?.takeIf { it.isNotEmpty() }, status, retry)
            }
            return response
        } catch (e: ApiException) {
            throw e
        } catch (e: IOException) {
            throw e
        } finally {
            connection.disconnect()
        }
    }
}
