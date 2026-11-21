package vn.shelftap.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Paginated response wrapper returned by GET /api/titles.
 *
 * @property items List of titles for the requested page.
 * @property page Zero-based current page index.
 * @property size Page size limit (maximum 50).
 * @property total Total number of matching titles in the catalogue.
 */
data class TitleListResponse(
    @SerializedName("items")
    val items: List<Title>,

    @SerializedName("page")
    val page: Int,

    @SerializedName("size")
    val size: Int,

    @SerializedName("total")
    val total: Int
)
