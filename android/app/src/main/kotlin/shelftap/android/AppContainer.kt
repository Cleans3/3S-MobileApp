package shelftap.android

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map
import okhttp3.HttpUrl.Companion.toHttpUrl
import shelftap.android.api.AuthApi
import shelftap.android.api.Net
import shelftap.android.api.ShelfTapApi
import shelftap.android.data.DeviceIdStore
import shelftap.android.data.RecommendPrefs
import shelftap.android.data.Repository
import shelftap.android.data.RepositoryRecommendSource
import shelftap.android.data.ThemeStore
import shelftap.android.security.IntegrityChecker
import shelftap.android.security.KeystoreRefreshTokenVault
import shelftap.android.recommend.RecommendationService
import shelftap.android.reminder.AndroidReminderAlarms
import shelftap.android.reminder.ReminderPrefs
import shelftap.android.reminder.ReminderService
import shelftap.android.reminder.SessionCue
import shelftap.android.reminder.notificationsAllowed
import shelftap.android.security.RefreshTokenVault
import shelftap.android.session.InactivityTracker
import shelftap.android.session.SessionManager

/** Hand-wired dependencies; one instance per process. */
class AppContainer(private val context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val baseUrl = BuildConfig.API_BASE_URL.toHttpUrl()

    private val dataStore = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("shelftap") }
    val vault: RefreshTokenVault = KeystoreRefreshTokenVault(dataStore)
    val integrity = IntegrityChecker(context)
    val themeStore = ThemeStore(dataStore)
    val recommendPrefs = RecommendPrefs(dataStore)
    val reminderPrefs = ReminderPrefs(dataStore)

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

    /**
     * The suggestions for the signed-in member (computed on the phone, kept in memory, dropped when the session ends). Built on first use,
     * from whatever repository the container holds then.
     */
    val recommender: RecommendationService by lazy {
        RecommendationService(RepositoryRecommendSource(repository), recommendPrefs.useHistory).also { service ->
            service.bind(appScope, session.state.map { it is shelftap.android.session.SessionState.Active })
        }
    }

    /**
     * The due-date reminders of the signed-in member: worked out on the phone from the loans, set as alarms of the phone, dropped when the
     * setting goes off, when Android stops allowing notifications and when the session ends. Built on first use.
     */
    val reminders: ReminderService by lazy {
        ReminderService(reminderPrefs, AndroidReminderAlarms(context.applicationContext), repository::loans, { notificationsAllowed(context) }).also { service ->
            service.bind(appScope, session.state.map { state ->
                when (state) {
                    is shelftap.android.session.SessionState.Active -> SessionCue.ACTIVE
                    // Signed out by the member, by the server (token reused) or by a lock key that no longer works: no reminder may outlive it.
                    // A timeout or an expired session is the same member on the same phone, so their alarms stay.
                    is shelftap.android.session.SessionState.LoggedOut ->
                        if (state.reason == null || state.reason == shelftap.android.session.LogoutReason.TOKEN_REUSED ||
                            state.reason == shelftap.android.session.LogoutReason.LOCK_KEY_INVALIDATED) SessionCue.ENDED else SessionCue.PAUSED
                    else -> SessionCue.PAUSED
                }
            })
        }
    }

    /** A subject picked on a book's page: the catalog takes it as its search the next time it is on screen, then clears it. */
    val subjectSearch = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    val inactivity = InactivityTracker(BuildConfig.INACTIVITY_LIMIT_MS, android.os.SystemClock::elapsedRealtime)
}
