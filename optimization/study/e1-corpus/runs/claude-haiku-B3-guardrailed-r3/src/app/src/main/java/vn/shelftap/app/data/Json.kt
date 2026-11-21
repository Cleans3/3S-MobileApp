package vn.shelftap.app.data

import kotlinx.serialization.json.Json

/** Shared JSON settings: tolerate extra fields the API may add. */
val ShelfTapJson: Json = Json {
    ignoreUnknownKeys = true
}
