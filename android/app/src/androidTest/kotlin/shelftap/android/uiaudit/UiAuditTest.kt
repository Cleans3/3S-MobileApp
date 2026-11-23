package shelftap.android.uiaudit

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
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
        runBlocking { session.restore() } // empty vault -> LoggedOut

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var activity: MainActivity
            scenario.onActivity { activity = it }

            waitFor("Log in with library card")
            audit("login", activity)

            runBlocking { session.login(AuditData.EMAIL, "audit-only") }
            waitFor(AuditData.titles.first().title)
            audit("catalog", activity)

            compose.onAllNodesWithText(AuditData.titles.first().title).onFirst().performClick()
            waitFor("ISBN ${AuditData.titles.first().isbn}")
            audit("detail", activity)
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

            tab("Loans"); waitFor(AuditData.loans.first().title)
            audit("loans", activity)

            // The emulator has no NFC, so Checkout shows its "This phone has no NFC." state; a phone with NFC shows "Self-checkout".
            tab("Checkout"); waitFor("Self-checkout", "NFC")
            audit("checkout", activity)

            tab("Reserved"); waitFor(AuditData.reservations.first().title)
            audit("reservations", activity)

            tab("Account"); waitFor(AuditData.me.fullName)
            audit("account", activity)
        }
    }

    private fun tab(name: String) {
        compose.onNodeWithText(name).performClick()
        compose.waitForIdle()
    }

    private fun waitFor(vararg texts: String) {
        compose.waitUntil(15_000) {
            texts.any { compose.onAllNodes(hasText(it, substring = true)).fetchSemanticsNodes().isNotEmpty() }
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
        val params = Parameters().apply { putScreenCapture(BitmapImage(bitmap)) }
        val checks = AccessibilityCheckPreset.getAccessibilityHierarchyChecksForPreset(AccessibilityCheckPreset.LATEST)
        File(outDir, "checks.txt").writeText(checks.joinToString("\n") { it.javaClass.simpleName } + "\n") // what "0 findings" was checked against
        val results: List<AccessibilityHierarchyCheckResult> =
            checks
                .flatMap { it.runCheckOnHierarchy(hierarchy, null, params) }
                .filter { it.type == AccessibilityCheckResultType.ERROR || it.type == AccessibilityCheckResultType.WARNING }

        val tsv = File(outDir, "findings-$config.tsv")
        if (!tsv.exists()) tsv.writeText("config\tscreen\tcheck\ttype\telement\tbounds\tmessage\n")
        tsv.appendText(
            results.joinToString("") { r ->
                val e = r.element
                val what = listOfNotNull(e?.className?.toString()?.substringAfterLast('.'), e?.text?.toString(), e?.contentDescription?.toString())
                    .joinToString(" | ")
                listOf(config, screen, r.sourceCheckClass.simpleName, r.type.name, what, e?.boundsInScreen?.toString().orEmpty(), r.getMessage(Locale.ENGLISH).toString())
                    .joinToString("\t") { it.replace('\t', ' ').replace('\n', ' ') } + "\n"
            },
        )
        File(outDir, "elements-$config.tsv").appendText(elementRows)
        File(outDir, "screens-$config.tsv").appendText("$config\t$screen\t$elements\t${results.size}\n")
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
