package vn.shelftap.app

import android.app.Application
import vn.shelftap.app.data.local.PreferenceManager
import vn.shelftap.app.data.repository.AuthRepository

class ShelfTapApp : Application() {

    lateinit var preferenceManager: PreferenceManager
        private set

    lateinit var authRepository: AuthRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferenceManager = PreferenceManager(this)
        authRepository = AuthRepository(preferenceManager)
    }

    companion object {
        lateinit var instance: ShelfTapApp
            private set
    }
}
