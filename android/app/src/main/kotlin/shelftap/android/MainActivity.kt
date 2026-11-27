package shelftap.android

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import shelftap.android.data.ThemeMode
import shelftap.android.session.LogoutReason
import shelftap.android.session.SessionState
import shelftap.android.ui.AppRoot
import shelftap.android.ui.LocalContainer
import shelftap.android.ui.theme.EffectTier
import shelftap.android.ui.theme.LocalEffectTier
import shelftap.android.ui.theme.ShelfTapTheme
import shelftap.android.ui.theme.rememberEffectTier

/** FragmentActivity because BiometricPrompt needs one. Hardening is set here once, for every screen. */
class MainActivity : FragmentActivity() {
    private val container get() = (application as ShelfTapApp).container
    private val handler = Handler(Looper.getMainLooper())
    private val idleCheck = object : Runnable {
        override fun run() {
            logoutIfIdle()
            handler.postDelayed(this, IDLE_POLL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // The system's starting window (teal, the book) leaves as soon as the first frame is drawn, with no fade or zoom of its own: the app's start
        // screen (StartupScreen) is the same teal and plays the board's entrance right after it.
        installSplashScreen().setOnExitAnimationListener { it.remove() }
        super.onCreate(savedInstanceState)
        // No screenshots, no screen recording, no Recents thumbnail: every screen shows personal data or a password field.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) setRecentsScreenshotEnabled(false)
        setContent {
            val mode by container.themeStore.mode.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val forcedName by container.themeStore.forcedEffect.collectAsStateWithLifecycle(null)
            // The forced tier is a debug tool for A/B on one phone; a release build never reads it.
            val forced = if (BuildConfig.DEBUG) EffectTier.entries.firstOrNull { it.name == forcedName } else null
            val tier = rememberEffectTier(forced)
            CompositionLocalProvider(LocalContainer provides container, LocalEffectTier provides tier) {
                ShelfTapTheme(mode) { AppRoot() }
            }
        }
        // Tapjacking: ignore touches while another window covers ours.
        window.decorView.filterTouchesWhenObscured = true
    }

    override fun onStart() {
        super.onStart()
        logoutIfIdle()
        handler.postDelayed(idleCheck, IDLE_POLL_MS)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        container.inactivity.touch()
    }

    override fun onStop() {
        handler.removeCallbacks(idleCheck)
        // A rotation also stops the activity; only a real trip to the background locks.
        if (!isChangingConfigurations) container.appScope.launch { container.session.lockIfEnabled() }
        super.onStop()
    }

    /** Idle time keeps running while the app is in the background, so a long absence logs out on return. */
    private fun logoutIfIdle() {
        if (!container.inactivity.expired()) return
        val state = container.session.state.value
        if (state is SessionState.Active || state is SessionState.PasswordChangeRequired) {
            container.appScope.launch { container.session.logout(LogoutReason.INACTIVITY) }
            container.inactivity.touch()
        }
    }

    private companion object {
        const val IDLE_POLL_MS = 15_000L
    }
}
