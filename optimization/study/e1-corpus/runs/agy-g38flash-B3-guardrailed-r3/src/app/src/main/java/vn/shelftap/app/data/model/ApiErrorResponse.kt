package vn.shelftap.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorResponse(
    val code: String? = null,
    val message: String? = null
)
