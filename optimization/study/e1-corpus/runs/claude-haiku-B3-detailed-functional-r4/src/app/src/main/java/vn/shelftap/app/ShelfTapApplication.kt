package vn.shelftap.app

import android.app.Application
import android.content.Context
import vn.shelftap.app.auth.AuthRepository
import vn.shelftap.app.data.LocalCache
import vn.shelftap.app.data.ShelfRepository
import vn.shelftap.app.data.ShelfTapApi

class ShelfTapApplication : Application() {

    // Placeholder. Replace with the member app's existing AuthRepository implementation.
    private val authRepository: AuthRepository = object : AuthRepository {
        override suspend fun accessToken(): String =
            error("AuthRepository implementation is not wired in ShelfTapApplication")
    }

    val repository: ShelfRepository by lazy {
        ShelfRepository(
            api = ShelfTapApi(authRepository),
            cache = LocalCache(getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)),
        )
    }

    private companion object {
        const val CACHE_PREFS = "shelftap_cache"
    }
}

fun Context.shelfRepository(): ShelfRepository =
    (applicationContext as ShelfTapApplication).repository
