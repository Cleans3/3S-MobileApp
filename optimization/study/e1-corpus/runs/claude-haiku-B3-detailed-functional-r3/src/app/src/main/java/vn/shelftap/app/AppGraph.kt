package vn.shelftap.app

import android.content.Context
import kotlinx.serialization.json.Json
import vn.shelftap.app.auth.AuthRepository
import vn.shelftap.app.data.SavedDataStore
import vn.shelftap.app.data.ShelfTapApi
import vn.shelftap.app.data.ShelfTapRepository

/**
 * Minimal manual dependency container.
 *
 * [authRepository] is the app's existing AuthRepository (vn.shelftap.app.auth). It must be
 * assigned by the app's sign-in code before any screen that loads data is shown.
 */
object AppGraph {
    lateinit var authRepository: AuthRepository

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val repository: ShelfTapRepository by lazy {
        val json = Json { ignoreUnknownKeys = true }
        ShelfTapRepository(
            api = ShelfTapApi(json),
            store = SavedDataStore(appContext, json),
            accessToken = { authRepository.accessToken() },
        )
    }
}
