package shelftap.android.uiaudit

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.graphics.RenderNode
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.view.View
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import shelftap.android.ui.StartupScreen
import shelftap.android.ui.CardLoginContent
import shelftap.android.ui.ChangePasswordContent
import shelftap.android.ui.theme.AdaptiveSize
import shelftap.android.ui.SignInSplitFrame
import shelftap.android.ui.theme.Spacing
import androidx.compose.foundation.layout.BoxWithConstraints
import shelftap.android.ui.LockContent
import shelftap.android.ui.OfflineContent
import shelftap.android.ui.CheckoutViewModel
import shelftap.android.ui.DoneCard
import shelftap.android.ui.FailedCard
import shelftap.android.ui.NfcStage
import shelftap.android.ui.SendingCard
import shelftap.android.ui.StepUpCard
import shelftap.android.ui.theme.EffectTier
import shelftap.android.ui.theme.LocalEffectTier
import shelftap.android.ui.theme.ShelfTapTheme
import shelftap.android.ui.uiText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityHierarchyCheckResult
import com.google.android.apps.common.testing.accessibility.framework.Parameters
import com.google.android.apps.common.testing.accessibility.framework.uielement.AccessibilityHierarchyAndroid
import com.google.android.apps.common.testing.accessibility.framework.utils.contrast.BitmapImage
import java.io.File
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import shelftap.android.AppContainer
import shelftap.android.MainActivity
import shelftap.android.R
import shelftap.android.session.LogoutReason
import shelftap.android.ShelfTapApp
import shelftap.android.data.Repository
import shelftap.android.session.SessionManager

/**
 * UI audit for optimization/ui-ux-plan.md §8.2: the real app (MainActivity, its theme and navigation) on a device,
 * fed by [AuditData] instead of a server. On each main screen it runs the Accessibility Test Framework checks (the
 * engine behind Accessibility Scanner) on the live accessibility tree plus a rendered screenshot, and saves both.
 *
 * Measurement only: skipped unless the instrumentation argument `uiAudit` (a label such as `A`) is set. Run it through
 * `optimization/ui-ux/audit/run_audit.ps1`, which sets the device configuration (dark mode, font scale, width) for each
 * `config` and pulls the results from `<externalFilesDir>/ui-audit/<label>/` to the host.
 *
 * The container's session, vault and repository are swapped by reflection, so the code under audit is the unchanged
 * product code. FLAG_SECURE stays set by MainActivity; screenshots are drawn from the view tree in-process
 * (`View.draw`), which FLAG_SECURE does not affect, and are never taken through the system.
 */
@RunWith(AndroidJUnit4::class)
class UiAuditTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val args = InstrumentationRegistry.getArguments()
    private val label = args.getString("uiAudit").orEmpty()
    private val config = args.getString("config") ?: "light"
    private val uiAutomation by lazy {
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).apply {
            serviceInfo = serviceInfo.apply { flags = flags or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS }
        }
    }
    private val outDir by lazy { File(instrumentation.targetContext.getExternalFilesDir(null), "ui-audit/$label").apply { mkdirs() } }

    @Before
    fun gate() = assumeTrue("pass -Pandroid.testInstrumentationRunnerArguments.uiAudit=<label> to run the UI audit", label.isNotEmpty())

    @Test
    fun tour() {
        val app = instrumentation.targetContext.applicationContext as ShelfTapApp
        val session = installFakes(app.container)
        // The effect tier the run is made at (FULL, MEDIUM or LIGHT); the phone's own choice when none is given. Always set, because the setting is kept between runs.
        runBlocking { app.container.themeStore.setForcedEffect(args.getString("tier")) }
        runBlocking { session.restore() } // empty vault -> LoggedOut

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var activity: MainActivity
            scenario.onActivity { activity = it }

            // Logged out with no reason: the welcome screen first, then the catalog as a guest, and the login one step away in the menu.
            waitFor(str(R.string.welcome_guest))
            audit("welcome", activity)
            compose.onNodeWithText(str(R.string.welcome_guest)).performClick()
            waitFor(str(R.string.catalog_theme)) // the banner: always on screen, unlike the strips below it at a large font
            audit("guest", activity)
            openMenu(); loginEntry()
            waitFor(str(R.string.login_with_card))
            audit("login", activity)

            runBlocking { session.login(AuditData.EMAIL, "audit-only") }
            waitFor(str(R.string.catalog_theme)) // the banner: always on screen, unlike the strips below it at a large font
            audit("catalog", activity)
            // Mid-scroll: the page passes under the top bar and the floating button.
            compose.onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeUp() }
            compose.waitForIdle()
            captureHardware(activity.window.decorView)?.let { bmp -> File(outDir, "$config/catalog-scrolled-hw.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            compose.onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeDown() }
            compose.waitForIdle()
            holdShot("press-burger", compose.onAllNodesWithContentDescription(str(R.string.menu_open)), activity)
            holdShot("press-card", compose.onAllNodes(hasContentDescription(AuditData.titles.first().title, substring = true)), activity, scroll = true)

            // "See all books" opens the list: the search field and the row of subject chips on glass over the rows.
            // The button is the last item of a lazy column, so it exists only once scrolled to: scroll the list to that item.
            compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText(str(R.string.catalog_see_all_books)))
            compose.waitForIdle()
            compose.onNodeWithText(str(R.string.catalog_see_all_books)).performScrollTo().performClick()
            waitFor(str(R.string.catalog_chip_all))
            audit("list", activity)
            // A chip searches its subject (the second chip is the first subject).
            compose.onAllNodes(hasText(str(R.string.catalog_chip_all)).not() and isSelectable()).onFirst().performClick()
            compose.waitForIdle()
            audit("list-subject", activity)
            // The back button of the bar (the menu button turned into ←) closes the list and returns to the catalog's home.
            // (A wide window has no top bar with a back button: there the back gesture does it.)
            if (hasBurger() || compose.onAllNodesWithContentDescription(str(R.string.back_to_catalog)).fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithContentDescription(str(R.string.back_to_catalog)).performClick()
            } else {
                scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            }
            waitFor(str(R.string.catalog_theme))

            openFirstTitle()
            waitFor(AuditData.titles.first().isbn) // the ISBN tile of the book page
            audit("detail", activity)
            // On a wide window the detail is a pane beside the list, not a screen to go back from.
            if (config == "expanded") {
                // The wide menu folds into the rail and opens again; the book's pane closes and gives its room back to the list.
                compose.onNodeWithContentDescription(str(R.string.nav_collapse)).performClick(); compose.waitForIdle()
                audit("nav-folded", activity)
                compose.onNodeWithContentDescription(str(R.string.nav_expand)).performClick(); compose.waitForIdle()
                compose.onNodeWithContentDescription(str(R.string.title_close_pane)).performClick(); compose.waitForIdle()
                audit("pane-closed", activity)
            } else {
                scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            }

            tab(R.string.tab_loans); waitFor(AuditData.loans.first().title)
            audit("loans", activity)

            // The emulator has no NFC, so Checkout shows its "This phone has no NFC." state; a phone with NFC shows "Self-checkout".
            compose.onNode(hasText(str(R.string.checkout_pill)) or hasContentDescription(str(R.string.tab_checkout))).performClick()
            // A phone with NFC (or with NFC switched off) shows other words than the emulator's: wait for any of them, and go on when none comes (the shot is taken anyway).
            runCatching { waitFor(str(R.string.checkout_title), str(R.string.nfc_missing)) }
            compose.waitForIdle()
            audit("checkout", activity)
            // The emulator has no NFC, so the states of the self-checkout are drawn on their own, over the app.
            overlayShot("checkout-wait", activity) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    NfcStage()
                    Text(str(R.string.checkout_hold), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                }
            }
            overlayShot("history-stepup", activity) { StepUpCard(onLogin = {}) }
            overlayShot("password-first", activity, pad = 0.dp) {
                // as the app draws it: beside the shelves in a wide window
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val split = maxWidth >= AdaptiveSize.signInSplitMin
                    val content: @Composable () -> Unit = { ChangePasswordContent(forced = true, busy = false, error = null, onSave = { _, _ -> }, onLogout = {}, split = split) }
                    if (split) SignInSplitFrame(content) else content()
                }
            }
            overlayShot("lock", activity) { LockContent(onUnlock = {}, onLogout = {}) }
            overlayShot("offline", activity) { OfflineContent(busy = false, tries = 2, onRetry = {}, onLogout = {}) }
            // The entrance of the start screen, frame by frame (the composition clock is advanced by hand): 120 ms, 300 ms, then done.
            overlayShot("splash-120ms", activity, pad = 0.dp, advanceMs = 120) { StartupScreen() }
            overlayShot("splash-300ms", activity, pad = 0.dp, advanceMs = 300) { StartupScreen() }
            overlayShot("splash-600ms", activity, pad = 0.dp, advanceMs = 600) { StartupScreen() }
            overlayShot("splash-1000ms", activity, pad = 0.dp, advanceMs = 1000) { StartupScreen() }
            overlayShot("splash", activity, pad = 0.dp) { StartupScreen() }
            overlayShot("login-card", activity) { CardLoginContent(busy = false, error = null, onBack = {}) }
            overlayShot("login-card-busy", activity) { CardLoginContent(busy = true, error = null, onBack = {}) }
            overlayShot("checkout-sending", activity) { SendingCard() }
            overlayShot("checkout-done", activity) { DoneCard(AuditData.loans[1], onAgain = {}, onLoans = {}) }
            overlayShot("checkout-failed", activity) { FailedCard(CheckoutViewModel.UiState.Failed(uiText(R.string.err_unpaid_fine), unpaidFine = true), onAgain = {}, onLoans = {}) }

            tab(R.string.tab_reservations); waitFor(AuditData.reservations.first().title)
            audit("reservations", activity)
            // Cancelling asks first, inside the card.
            compose.onNodeWithContentDescription(str(R.string.reservations_cancel_for, AuditData.reservations.first().title)).performClick()
            waitFor(str(R.string.reservations_cancel_ask, AuditData.reservations.first().title))
            audit("reservations-asking", activity)
            compose.onNodeWithText(str(R.string.reservations_keep)).performClick()
            compose.waitForIdle()

            tab(R.string.tab_account); waitFor(AuditData.me.fullName)
            audit("account", activity)
            // The foot of the page too: its theme choice and the sign-out button sit below the first screen, and the scanner only judges what is on screen.
            // The page is the scrollable that holds the sign-out button, not the first one: on a medium window the navigation rail scrolls too, and comes first.
            val accountPage = hasScrollAction() and hasAnyDescendant(hasText(str(R.string.action_logout)))
            compose.onNode(accountPage).performScrollToNode(hasText(str(R.string.action_logout)))
            // Scrolling to a node stops as soon as it shows; push on to the real end of the page, where the padding clears the floating button.
            repeat(3) { compose.onNode(accountPage).performTouchInput { swipeUp() } }
            compose.waitForIdle()
            audit("account-bottom", activity)
            // (on a wide window the history link is in the pane beside the list, not in the list)
            compose.onNode(hasScrollAction() and hasAnyDescendant(hasText(str(R.string.account_history)))).performScrollToNode(hasText(str(R.string.account_history)))
            compose.waitForIdle()
            compose.onNodeWithText(str(R.string.account_history)).performClick(); waitFor(AuditData.history.first().title)
            audit("history", activity)
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            compose.waitForIdle()

            menuFrames(activity)
            openMenu()
            audit("menu", activity)

            // Signed out for each reason, by the real path: the session logs out and the login is what comes next, with the notice of that reason.
            for (reason in LogoutReason.values()) {
                runBlocking { session.logout(reason) }
                waitFor(str(R.string.login_again))
                audit("signedout-${reason.name.lowercase()}", activity)
                runBlocking { session.login(AuditData.EMAIL, "audit-only") }
                waitFor(str(R.string.catalog_theme))
            }
        }
    }

    /**
     * A composable drawn on its own over the whole window (the self-checkout states cannot be reached on an emulator without NFC),
     * captured by the GPU renderer as `<config>/<name>-hw.png`, then removed. The frame clock is advanced by hand so that endless
     * animations (the rings) do not keep the test from going idle.
     */
    private fun overlayShot(name: String, activity: MainActivity, pad: androidx.compose.ui.unit.Dp = 24.dp, advanceMs: Long = 900, content: @Composable () -> Unit) {
        lateinit var view: ComposeView
        instrumentation.runOnMainSync {
            view = ComposeView(activity).apply {
                setContent {
                    CompositionLocalProvider(LocalEffectTier provides EffectTier.FULL) {
                        ShelfTapTheme {
                            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(pad), contentAlignment = Alignment.Center) { content() }
                        }
                    }
                }
            }
            (activity.window.decorView as ViewGroup).addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(advanceMs)
        captureHardware(activity.window.decorView)?.let { bmp -> File(outDir, "$config/$name-hw.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        compose.mainClock.autoAdvance = true
        instrumentation.runOnMainSync { (activity.window.decorView as ViewGroup).removeView(view) }
        compose.waitForIdle()
    }

    /**
     * The menu opening and closing, frame by frame: the compose clock is stopped and advanced by hand, and the GPU renderer captures the
     * window at each step (`menu-open-<ms>-hw.png`, `menu-close-<ms>-hw.png`), so the in-between frames can be looked at.
     */
    private fun menuFrames(activity: MainActivity) {
        val burger = compose.onAllNodesWithContentDescription(str(R.string.menu_open))
        if (burger.fetchSemanticsNodes().isEmpty()) return
        compose.mainClock.autoAdvance = false
        val steps = listOf(30, 60, 60, 80, 120) // cumulative: 30, 90, 150, 230, 350 ms
        var at = 0
        burger[0].performClick()
        for (d in steps) {
            compose.mainClock.advanceTimeBy(d.toLong()); at += d
            captureHardware(activity.window.decorView)?.let { bmp -> File(outDir, "$config/menu-open-$at-hw.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        }
        compose.onAllNodesWithContentDescription(str(R.string.menu_close))[0].performClick()
        at = 0
        for (d in listOf(15, 15, 15, 15, 15, 15, 30, 50, 50, 60)) { // fine steps first: the panel is gone within 90 ms
            compose.mainClock.advanceTimeBy(d.toLong()); at += d
            captureHardware(activity.window.decorView)?.let { bmp -> File(outDir, "$config/menu-close-$at-hw.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
    }

    /** A control held down: the pressed state, drawn by the GPU renderer, saved as `<config>/<name>-hw.png`. Skipped when the control is not on screen. */
    private fun holdShot(name: String, nodes: androidx.compose.ui.test.SemanticsNodeInteractionCollection, activity: MainActivity, scroll: Boolean = false) {
        if (nodes.fetchSemanticsNodes().isEmpty()) return
        if (scroll) nodes[0].performScrollTo()
        nodes[0].performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(400) // inside a scrolling row the press shows after the tap timeout; then the veil and the shrink run to their held state
        compose.waitForIdle()
        captureHardware(activity.window.decorView)?.let { bmp -> File(outDir, "$config/$name-hw.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        nodes[0].performTouchInput { cancel() } // cancel: a release would click
        compose.waitForIdle()
    }

    /** Text as the app shows it in the device's current language, so the tour works in Vietnamese and in English. */
    private fun str(id: Int, vararg args: Any) = instrumentation.targetContext.getString(id, *args)

    /** The ☰ menu exists on narrow windows only; on a medium or wide window the navigation is always on screen. */
    private fun openMenu() {
        val burger = compose.onAllNodesWithContentDescription(str(R.string.menu_open))
        if (burger.fetchSemanticsNodes().isNotEmpty()) burger[0].performClick()
        compose.waitForIdle()
    }

    /** Opens the menu and picks the entry; the title bar shows the current screen's name too, so the menu entry is the last match. */
    /** The first book: a card of the home (named "title, author, status" for TalkBack) or a row of the list. */
    private fun openFirstTitle() {
        val title = AuditData.titles.first().title
        val cards = compose.onAllNodes(hasContentDescription(title, substring = true))
        // At a large font the banner pushes the strips below the fold, and a lazy list composes only what is on screen: scroll to them.
        repeat(4) {
            if (cards.fetchSemanticsNodes().isEmpty() && compose.onAllNodesWithText(title).fetchSemanticsNodes().isEmpty()) {
                compose.onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeUp() }
                compose.waitForIdle()
            }
        }
        if (cards.fetchSemanticsNodes().isNotEmpty()) {
            // Bring the card clear of the floating button before touching it: its centre can sit under the button.
            cards[0].performScrollTo()
            cards[0].performSemanticsAction(SemanticsActions.OnClick) // the action, not a touch: at a large font the card can lie under the button
        } else {
            compose.onAllNodesWithText(title).onFirst().performClick()
        }
    }

    /** The ☰ button is on screen, open (its label is "close menu") or shut. */
    private fun hasBurger() =
        compose.onAllNodesWithContentDescription(str(R.string.menu_open)).fetchSemanticsNodes().isNotEmpty() ||
            compose.onAllNodesWithContentDescription(str(R.string.menu_close)).fetchSemanticsNodes().isNotEmpty()

    /** Guest: the login entry of the menu, or of the always-visible navigation. */
    private fun loginEntry() {
        val burger = hasBurger()
        if (burger) compose.onAllNodesWithText(str(R.string.login_action)).onLast().performClick()
        else compose.onNodeWithContentDescription(str(R.string.nav_go_to, str(R.string.login_action))).performClick()
        compose.waitForIdle()
    }

    private fun tab(id: Int) {
        val burger = hasBurger()
        if (burger) {
            openMenu()
            compose.onAllNodesWithText(str(id)).onLast().performClick()
        } else {
            // The always-visible navigation reads each entry as "Go to <name>".
            compose.onNodeWithContentDescription(str(R.string.nav_go_to, str(id))).performClick()
        }
        compose.waitForIdle()
    }

    private fun waitFor(vararg texts: String) {
        compose.waitUntil(15_000) {
            texts.any { compose.onAllNodes(hasText(it, substring = true) or hasContentDescription(it, substring = true)).fetchSemanticsNodes().isNotEmpty() }
        }
        compose.waitForIdle()
    }

    private fun audit(screen: String, activity: MainActivity) {
        compose.waitForIdle()
        lateinit var bitmap: Bitmap
        instrumentation.runOnMainSync {
            val root: View = activity.window.decorView
            bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
            root.draw(Canvas(bitmap))
        }
        File(outDir, config).mkdirs()
        File(outDir, "$config/$screen.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        // The software draw above cannot show what the GPU draws (blur, backdrop layers, shadows). The same tree drawn by the hardware
        // renderer into an off-screen buffer can; it is saved beside it, and the checks run on it too (findings-hw-<config>.tsv).
        val hardware = captureHardware(activity.window.decorView)
        if (hardware != null) File(outDir, "$config/$screen-hw.png").outputStream().use { hardware.compress(Bitmap.CompressFormat.PNG, 100, it) }

        // The tree comes from the system through UiAutomation, as Accessibility Scanner gets it; FLAG_SECURE does not hide it.
        var rootNode = uiAutomation.rootInActiveWindow
        for (attempt in 1..20) {
            if (rootNode != null) break
            Thread.sleep(500) // the window can be momentarily inactive right after launch or a configuration change
            rootNode = uiAutomation.rootInActiveWindow
        }
        rootNode ?: error("no accessibility root for $screen")
        val hierarchy = AccessibilityHierarchyAndroid.newBuilder(rootNode, instrumentation.targetContext).build()
        val elements = hierarchy.activeWindow.allViews.size
        val elementRows = hierarchy.activeWindow.allViews.joinToString("") { e ->
            "$config\t$screen\t${e.className}\t${e.text}\t${e.contentDescription}\t${e.boundsInScreen}\tclickable=${e.isClickable}\n"
        }
        val checks = AccessibilityCheckPreset.getAccessibilityHierarchyChecksForPreset(AccessibilityCheckPreset.LATEST)
        File(outDir, "checks.txt").writeText(checks.joinToString("\n") { it.javaClass.simpleName } + "\n") // what "0 findings" was checked against
        fun run(image: Bitmap): List<AccessibilityHierarchyCheckResult> {
            val params = Parameters().apply { putScreenCapture(BitmapImage(image)) }
            return checks
                .flatMap { it.runCheckOnHierarchy(hierarchy, null, params) }
                .filter { it.type == AccessibilityCheckResultType.ERROR || it.type == AccessibilityCheckResultType.WARNING }
        }
        // The page scrolls under the top bar and under the floating button, and the soft band there fades what passes under it (by design,
        // plan 5.5): text there is lighter than its own colour, which the checker cannot know. A finding on an element that reaches into
        // those bands is marked `under_bar` (the last column), so it can be counted apart from a finding on the page itself.
        val density = activity.resources.displayMetrics.density
        val bars = activity.window.decorView.rootWindowInsets?.getInsets(android.view.WindowInsets.Type.systemBars())
        val topBand = (bars?.top ?: 0) + Spacing.touch.value * density
        val bottomBand = bitmap.height - ((bars?.bottom ?: 0) + Spacing.pillClearance.value * density)
        fun underBar(r: AccessibilityHierarchyCheckResult): Boolean {
            val b = r.element?.boundsInScreen ?: return false
            return r.sourceCheckClass.simpleName == "TextContrastCheck" && (b.top < topBand || b.bottom > bottomBand)
        }
        fun write(name: String, rows: List<AccessibilityHierarchyCheckResult>) {
            val tsv = File(outDir, name)
            if (!tsv.exists()) tsv.writeText("config\tscreen\tcheck\ttype\telement\tbounds\tmessage\tunder_bar\n")
            tsv.appendText(
                rows.joinToString("") { r ->
                    val e = r.element
                    val what = listOfNotNull(e?.className?.toString()?.substringAfterLast('.'), e?.text?.toString(), e?.contentDescription?.toString())
                        .joinToString(" | ")
                    listOf(config, screen, r.sourceCheckClass.simpleName, r.type.name, what, e?.boundsInScreen?.toString().orEmpty(), r.getMessage(Locale.ENGLISH).toString(), if (underBar(r)) "yes" else "no")
                        .joinToString("\t") { it.replace('\t', ' ').replace('\n', ' ') } + "\n"
                },
            )
        }
        val results = run(bitmap)
        write("findings-$config.tsv", results)
        if (hardware != null) write("findings-hw-$config.tsv", run(hardware))
        File(outDir, "elements-$config.tsv").appendText(elementRows)
        File(outDir, "screens-$config.tsv").appendText("$config\t$screen\t$elements\t${results.size}\n")
    }

    /** The window's view tree drawn by the GPU renderer into an off-screen buffer (not the screen, so FLAG_SECURE is not involved). Null if the device cannot. */
    private fun captureHardware(root: View): Bitmap? = try {
        val w = root.width
        val h = root.height
        val reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 1, HardwareBuffer.USAGE_GPU_COLOR_OUTPUT or HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE)
        val node = RenderNode("ui-audit")
        node.setPosition(0, 0, w, h)
        instrumentation.runOnMainSync {
            val canvas = node.beginRecording(w, h)
            root.draw(canvas)
            node.endRecording()
        }
        val renderer = HardwareRenderer()
        renderer.setSurface(reader.surface)
        renderer.setContentRoot(node)
        renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()
        var image: android.media.Image? = null
        for (attempt in 1..20) { image = reader.acquireLatestImage(); if (image != null) break; Thread.sleep(50) }
        val buffer = image?.hardwareBuffer
        val copy = buffer?.let { Bitmap.wrapHardwareBuffer(it, null)?.copy(Bitmap.Config.ARGB_8888, false) }
        image?.close(); renderer.destroy(); reader.close(); node.discardDisplayList()
        copy
    } catch (e: Throwable) {
        android.util.Log.w("UiAudit", "no hardware capture: $e")
        null
    }

    /** Session over a fake auth API and an in-memory vault; repository over [AuditData]. Returns the new session. */
    private fun installFakes(container: AppContainer): SessionManager {
        val vault = AuditVault()
        val session = SessionManager(AuditData.AuthApiFake, vault, { "device-audit" }, { "ok" })
        val repository = Repository(AuditData.ApiFake, session)
        setField(container, "vault", vault)
        setField(container, "session", session)
        setField(container, "repository", repository)
        return session
    }

    private fun setField(target: Any, name: String, value: Any) {
        val f = target.javaClass.getDeclaredField(name)
        f.isAccessible = true
        f.set(target, value)
    }
}
