package vn.shelftap.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import vn.shelftap.app.data.AccountRepository
import vn.shelftap.app.data.ApiClient
import vn.shelftap.app.data.SessionStore

/** Manual dependency wiring. Blocking work is dispatched on [Dispatchers.IO] by the repository. */
class AppContainer(context: Context) {
    private val sessionStore = SessionStore(context.applicationContext)
    private val apiClient = ApiClient()

    val accountRepository = AccountRepository(apiClient, sessionStore, Dispatchers.IO)
}
