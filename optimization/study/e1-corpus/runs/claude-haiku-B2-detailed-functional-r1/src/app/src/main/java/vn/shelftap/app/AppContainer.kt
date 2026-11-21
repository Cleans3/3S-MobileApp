package vn.shelftap.app

import android.content.Context
import kotlinx.serialization.json.Json
import vn.shelftap.app.data.ApiClient
import vn.shelftap.app.data.SessionRepository
import vn.shelftap.app.data.SessionStore

/** Hand-rolled dependency container, created once per process by [ShelfTapApp]. */
class AppContainer(context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    private val store = SessionStore(context).also {
        // Creates the installation's deviceId on first launch; it is kept afterwards.
        it.deviceId()
    }

    val sessionRepository = SessionRepository(ApiClient(json), store)
}
