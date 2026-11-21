package vn.shelftap.app.data.model

import com.google.gson.annotations.SerializedName

data class PrivacyRequest(
    @SerializedName("historyEnabled") val historyEnabled: Boolean
)
