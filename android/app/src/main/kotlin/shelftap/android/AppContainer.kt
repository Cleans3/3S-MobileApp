package shelftap.android

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.HttpUrl.Companion.toHttpUrl
import shelftap.android.api.AuthApi
import shelftap.android.api.Net
import shelftap.android.api.ShelfTapApi
import shelftap.android.data.DeviceIdStore
import shelftap.android.data.Repository
import shelftap.android.data.ThemeStore
import shelftap.android.security.IntegrityChecker
import shelftap.android.security.KeystoreRefreshTokenVault
import shelftap.android.security.RefreshTokenVault
import shelftap.android.session.InactivityTracker
import shelftap.android.session.SessionManager

/** Hand-wired dependencies; one instance per process. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val baseUrl = BuildConfig.API_BASE_URL.toHttpUrl()

    private val dataStore = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("shelftap") }
    val vault: RefreshTokenVault = KeystoreRefreshTokenVault(dataStore)
    val integrity = IntegrityChecker(context)
    val themeStore = ThemeStore(dataStore)

    val baseClient = Net.baseClient(baseUrl, BuildConfig.API_PINS.split(',').filter { it.isNotBlank() })
    val session = SessionManager(
        authApi = Net.retrofit(baseClient, baseUrl).create(AuthApi::class.java),
        vault = vault,
        deviceIds = DeviceIdStore(dataStore),
        integrity = integrity,
    )
    val repository = Repository(
        Net.retrofit(Net.authedClient(baseClient, session), baseUrl).create(ShelfTapApi::class.java),
        session,
    )

    val inactivity = InactivityTracker(BuildConfig.INACTIVITY_LIMIT_MS, android.os.SystemClock::elapsedRealtime)
}
