package vn.shelftap.app

import android.app.Application

class ShelfTapApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ShelfTapGraph.repository = ShelfRepository(this, AuthRepositoryProvider.current)
    }
}

object ShelfTapGraph { lateinit var repository: ShelfRepository }

interface AuthRepository { suspend fun accessToken(): String }

object AuthRepositoryProvider {
    var current: AuthRepository = object : AuthRepository {
        override suspend fun accessToken(): String = ""
    }
}
