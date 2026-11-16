package shelftap.android.api

import java.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import retrofit2.HttpException

/** The `code` values of server/API.md. Clients branch on these, never on `detail`. */
enum class ErrorCode {
    VALIDATION_FAILED, INVALID_CREDENTIALS, ACCOUNT_LOCKED, TOKEN_INVALID, TOKEN_REUSED,
    PASSWORD_CHANGE_REQUIRED, FORBIDDEN, NOT_FOUND, CARD_NOT_ACTIVE, CARD_AUTH_FAILED,
    CHALLENGE_EXPIRED, COPY_NOT_AVAILABLE, LOAN_LIMIT_REACHED, UNPAID_FINE, RENEWAL_NOT_ALLOWED,
    RESERVED_BY_OTHER, ALREADY_RESERVED, RATE_LIMITED, STEP_UP_REQUIRED, KEY_ALREADY_REGISTERED, UNKNOWN;

    companion object {
        fun parse(code: String?): ErrorCode = entries.firstOrNull { it.name == code } ?: UNKNOWN
    }
}

@Serializable
private data class Problem(val code: String? = null)

/** Reads the `code` out of an application/problem+json body; anything else is UNKNOWN. */
fun problemCode(body: String?): ErrorCode =
    try {
        ErrorCode.parse(body?.let { Net.json.decodeFromString<Problem>(it).code })
    } catch (_: SerializationException) {
        ErrorCode.UNKNOWN
    }

/** Messages carry status and code only: never the server's `detail`, a URL or a token. */
sealed class ApiException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Http(val status: Int, val code: ErrorCode, val retryAfterSeconds: Long?) :
        ApiException("HTTP $status $code")

    class Network(cause: Throwable) : ApiException("network error", cause)
}

/** Runs a Retrofit call and maps every failure to [ApiException]. */
suspend fun <T> apiCall(block: suspend () -> T): T =
    try {
        block()
    } catch (e: HttpException) {
        val body = try { e.response()?.errorBody()?.string() } catch (_: IOException) { null }
        throw ApiException.Http(
            status = e.code(),
            code = problemCode(body),
            retryAfterSeconds = e.response()?.headers()?.get("Retry-After")?.toLongOrNull(),
        )
    } catch (e: IOException) {
        throw ApiException.Network(e)
    } catch (e: SerializationException) {
        throw ApiException.Network(e)
    }
