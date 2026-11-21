package vn.shelftap.app

import android.app.Application
import vn.shelftap.app.auth.UnavailableAuthRepository

class ShelfTapApp : Application() {

    /**
     * Replace [UnavailableAuthRepository] with the app's real AuthRepository.
     */
    val container: AppContainer by lazy {
        AppContainer(this, UnavailableAuthRepository)
    }
}
