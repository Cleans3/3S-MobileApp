package vn.shelftap.app

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import vn.shelftap.app.auth.AuthRepository
import vn.shelftap.app.auth.UnwiredAuthRepository
import vn.shelftap.app.data.LibraryRepository
import vn.shelftap.app.data.SecureCache
import vn.shelftap.app.net.ApiClient
import java.io.File

/** Manual dependency container. Dispatchers are injected so that tests can swap them. */
class AppContainer(
    context: Context,
    authRepository: AuthRepository = UnwiredAuthRepository(),
) {
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default

    val repository: LibraryRepository = LibraryRepository(
        api = ApiClient(authRepository, ioDispatcher),
        cache = SecureCache(File(context.applicationContext.filesDir, "secure-cache")),
        ioDispatcher = ioDispatcher,
    )
}
