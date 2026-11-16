package shelftap.android

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch
import shelftap.android.session.LogoutReason
import shelftap.android.session.SessionState
import shelftap.android.ui.AppRoot
import shelftap.android.ui.LocalContainer
import shelftap.android.ui.ShelfTapTheme

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
        super.onCreate(savedInstanceState)
        // No screenshots, no screen recording, no Recents thumbnail: every screen shows personal data or a password field.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) setRecentsScreenshotEnabled(false)
        setContent {
            CompositionLocalProvider(LocalContainer provides container) {
                ShelfTapTheme { AppRoot() }
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
