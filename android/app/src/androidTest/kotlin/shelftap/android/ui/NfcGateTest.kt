package shelftap.android.ui

import android.view.ViewGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import shelftap.android.MainActivity
import shelftap.android.nfc.NfcAvailability
import shelftap.android.nfc.TagReader
import shelftap.android.ui.theme.EffectTier
import shelftap.android.ui.theme.LocalEffectTier
import shelftap.android.ui.theme.ShelfTapTheme
import kotlin.test.assertEquals

/**
 * The card login shows [NfcGate] in place of its own screen when the phone has no NFC (or NFC is off), so the way back to the password
 * login has to be offered by the gate itself (`leave`): a gate that shows only its message leaves nobody a way out, most of all in a wide
 * window that has no menu beside the screen. These tests need a phone in that state: the emulator has no NFC, so they run there; on a phone
 * with NFC switched on they are skipped (assumption), and the "NFC ready" case runs instead.
 */
class NfcGateTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Before fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After fun close() {
        scenario.close()
    }

    private fun show(leave: (() -> Unit)?) {
        scenario.onActivity { activity ->
            val view = ComposeView(activity).apply {
                setContent {
                    CompositionLocalProvider(LocalEffectTier provides EffectTier.MEDIUM) {
                        ShelfTapTheme {
                            val way: (@androidx.compose.runtime.Composable () -> Unit)? = leave?.let { go -> { TextAction("Way out", onClick = go) } }
                            NfcGate(leave = way) { Text("Card reader") }
                        }
                    }
                }
            }
            (activity.window.decorView as ViewGroup).addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        compose.waitForIdle()
    }

    private fun availability() = TagReader.availability(instrumentation.targetContext)

    @Test fun withNoNfcTheGateStillOffersTheWayOut() {
        assumeTrue(availability() != NfcAvailability.READY)
        var left = 0
        show(leave = { left++ })
        compose.onNodeWithText("Way out").assertIsDisplayed()
        compose.onNodeWithText("Way out").performClick()
        assertEquals(1, left)
        // the card reader itself is not drawn: there is nothing to tap a card on
        compose.onNodeWithText("Card reader").assertDoesNotExist()
    }

    @Test fun withNfcOnTheGateShowsTheScreen() {
        assumeTrue(availability() == NfcAvailability.READY)
        show(leave = { })
        compose.onNodeWithText("Card reader").assertIsDisplayed()
    }

    @Test fun withNoWayOutGivenTheGateShowsOnlyItsMessage() {
        assumeTrue(availability() != NfcAvailability.READY)
        show(leave = null)
        compose.onNodeWithText("Way out").assertDoesNotExist()
    }
}
