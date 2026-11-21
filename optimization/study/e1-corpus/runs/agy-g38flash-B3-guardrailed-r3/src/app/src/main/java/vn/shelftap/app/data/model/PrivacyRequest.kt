package vn.shelftap.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PrivacyRequest(
    val historyEnabled: Boolean
)
