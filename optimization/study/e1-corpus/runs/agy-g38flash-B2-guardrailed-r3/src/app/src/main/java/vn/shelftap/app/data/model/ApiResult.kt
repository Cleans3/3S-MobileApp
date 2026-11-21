package vn.shelftap.app.data.model

sealed interface ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>
    data class Error(
        val code: String?,
        val statusCode: Int,
        val message: String?,
        val retryAfterSeconds: Int? = null
    ) : ApiResult<Nothing>
    data class NetworkError(val throwable: Throwable) : ApiResult<Nothing>
}
