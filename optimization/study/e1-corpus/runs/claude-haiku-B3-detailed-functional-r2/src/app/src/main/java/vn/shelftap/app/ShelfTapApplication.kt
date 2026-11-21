package vn.shelftap.app

import android.app.Application
import kotlinx.serialization.json.Json
import vn.shelftap.app.auth.AuthRepository
import vn.shelftap.app.data.ApiClient
import vn.shelftap.app.data.LibraryRepository
import vn.shelftap.app.data.SavedData

class ShelfTapApplication : Application() {

    val json: Json = Json { ignoreUnknownKeys = true }

    /**
     * Replace this placeholder with the app's existing AuthRepository implementation.
     * ShelfTap only reads the access token from it.
     */
    val auth: AuthRepository = object : AuthRepository {
        override suspend fun accessToken(): String =
            error("AuthRepository is not wired up")
    }

    val library: LibraryRepository by lazy {
        LibraryRepository(
            api = ApiClient(auth, json),
            saved = SavedData(filesDir, json),
        )
    }
}
