package vn.shelftap.app.data.remote

import retrofit2.Response

/**
 * Unwraps a Retrofit response, throwing a typed [ApiException] with parsed
 * JSON error body and Retry-After header if the HTTP response is unsuccessful.
 */
fun <T> Response<T>.unwrapBody(): T {
    if (isSuccessful) {
        val body = body()
        if (body != null) {
            return body
        }
        // Handle 204 No Content responses where body is Unit
        @Suppress("UNCHECKED_CAST")
        if (code() == 204) {
            return Unit as T
        }
        @Suppress("UNCHECKED_CAST")
        return Unit as T
    } else {
        val errorBodyString = try {
            errorBody()?.string()
        } catch (e: Exception) {
            null
        }
        val retryAfter = headers()["Retry-After"]?.toIntOrNull()
        throw ApiException.parse(errorBodyString, code(), retryAfter)
    }
}
