package vn.shelftap.app

import android.app.Application

class ShelfTapApplication : Application() {
    lateinit var repository: ShelfTapRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = ShelfTapRepository(SessionStore(this), ApiClient())
    }
}
