package vn.shelftap.app.data.model

import com.google.gson.annotations.SerializedName

data class PrivacyUpdateRequest(
    @SerializedName("historyEnabled") val historyEnabled: Boolean
)
