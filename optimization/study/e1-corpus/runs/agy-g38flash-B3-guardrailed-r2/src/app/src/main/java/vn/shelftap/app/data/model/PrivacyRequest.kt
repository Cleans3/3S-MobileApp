package vn.shelftap.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PrivacyRequest(
    @SerialName("historyEnabled") val historyEnabled: Boolean
)

@Serializable
data class ApiError(
    @SerialName("code") val code: String? = null,
    @SerialName("message") val message: String? = null
)
