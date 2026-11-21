package shelftap.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Collects the baseline profile for the shipped (B) journeys: home, then every tuned screen.
 * Run against the non-minified package (st.target shelftap.optlab.nor8) so rules carry original names;
 * AGP rewrites them for R8 when the file sits in optlab/src/main/baseline-prof.txt.
 */
@RunWith(AndroidJUnit4::class)
class ProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = Args.target,
        maxIterations = 6,
        stableIterations = 2,
        includeInStartupProfile = true,
    ) {
        for (factor in listOf("startup", "P01P09", "P06", "P13", "P03P04", "P08")) {
            pressHome()
            startActivityAndWait(launchIntent(Args.target, factor, "B"))
            journey(factor)
        }
    }
}
