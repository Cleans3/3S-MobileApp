package shelftap.android.ui.theme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import android.app.ActivityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.util.function.Consumer

/**
 * How much visual effect a phone is asked to pay for (plan §6.3). One app, three tiers, chosen at run time:
 * - [FULL]: Android 17+. Reserved for the progressive blur of the plan; today it draws what [MEDIUM] draws.
 * - [MEDIUM]: Android 12 to 16. Blur of the page behind a floating layer, fixed radius, only while the layer is open.
 * - [LIGHT]: Android 8 to 11, a low-RAM phone, battery saver, animations switched off, or blur switched off by the system.
 *   Translucent or solid layers, no blur, no motion.
 */
enum class EffectTier { FULL, MEDIUM, LIGHT }

/**
 * The one place that decides the tier from the phone's signals. Screens ask [LocalEffectTier], never the Android version.
 * [crossWindowBlur] is `null` when the system has no such setting (below Android 12).
 */
fun decideTier(sdkInt: Int, lowRam: Boolean, powerSave: Boolean, animationsOff: Boolean, crossWindowBlur: Boolean?): EffectTier = when {
    sdkInt < Build.VERSION_CODES.S -> EffectTier.LIGHT
    lowRam || powerSave || animationsOff -> EffectTier.LIGHT
    crossWindowBlur == false -> EffectTier.LIGHT
    sdkInt >= ANDROID_17 -> EffectTier.FULL
    else -> EffectTier.MEDIUM
}

/** A forced tier (debug builds only) can lower the effects but never ask for a blur the phone cannot draw. */
fun clampForced(forced: EffectTier, sdkInt: Int): EffectTier =
    if (sdkInt < Build.VERSION_CODES.S) EffectTier.LIGHT else forced

private const val ANDROID_17 = 37

val LocalEffectTier = staticCompositionLocalOf { EffectTier.LIGHT }

/**
 * The tier now, recomputed when battery saver, the system blur setting or the animation setting changes.
 * [forced] is the debug override; pass null in release builds.
 */
@Composable
fun rememberEffectTier(forced: EffectTier? = null): EffectTier {
    val context = LocalContext.current
    var powerSave by remember { mutableStateOf(isPowerSave(context)) }
    var animationsOff by remember { mutableStateOf(areAnimationsOff(context)) }
    var blurAllowed by remember { mutableStateOf<Boolean?>(crossWindowBlur(context)) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                powerSave = isPowerSave(c)
            }
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                animationsOff = areAnimationsOff(context)
            }
        }
        context.contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        val windowManager = context.getSystemService(WindowManager::class.java)
        val blurListener = Consumer<Boolean> { blurAllowed = it }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) windowManager.addCrossWindowBlurEnabledListener(context.mainExecutor, blurListener)
        onDispose {
            context.unregisterReceiver(receiver)
            context.contentResolver.unregisterContentObserver(observer)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) windowManager.removeCrossWindowBlurEnabledListener(blurListener)
        }
    }

    val lowRam = remember { context.getSystemService(ActivityManager::class.java).isLowRamDevice }
    val auto = decideTier(Build.VERSION.SDK_INT, lowRam, powerSave, animationsOff, blurAllowed)
    return forced?.let { clampForced(it, Build.VERSION.SDK_INT) } ?: auto
}

private fun isPowerSave(context: Context) = context.getSystemService(PowerManager::class.java).isPowerSaveMode

private fun areAnimationsOff(context: Context) =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

private fun crossWindowBlur(context: Context): Boolean? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) context.getSystemService(WindowManager::class.java).isCrossWindowBlurEnabled else null
