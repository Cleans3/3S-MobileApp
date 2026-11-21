package vn.shelftap.app

import android.app.Application
import vn.shelftap.app.data.repository.ShelfTapRepository
import vn.shelftap.app.data.storage.EncryptedSessionStorage

class ShelfTapApp : Application() {
    lateinit var repository: ShelfTapRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val sessionStorage = EncryptedSessionStorage(applicationContext)
        repository = ShelfTapRepository(sessionStorage)
    }
}
