package shelftap.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import shelftap.android.R

/**
 * Nunito (SIL OFL, `assets/licenses/Nunito-OFL.txt`), one variable font file with the weights the plan uses: 400, 600, 700.
 * It carries the stacked Vietnamese marks. minSdk is 26, which is where variable font axes start to work.
 */
val Nunito = FontFamily(
    listOf(400, 600, 700).map { w ->
        Font(R.font.nunito, weight = FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    },
)

private fun TextStyle.nunito(weight: Int) = copy(fontFamily = Nunito, fontWeight = FontWeight(weight))

/** The Material 3 scale in Nunito: 700 for display and headline, 600 for title and label, 400 for body. */
val ShelfTapTypography: Typography = Typography().let { m3 ->
    Typography(
        displayLarge = m3.displayLarge.nunito(700), displayMedium = m3.displayMedium.nunito(700), displaySmall = m3.displaySmall.nunito(700),
        headlineLarge = m3.headlineLarge.nunito(700), headlineMedium = m3.headlineMedium.nunito(700), headlineSmall = m3.headlineSmall.nunito(700),
        titleLarge = m3.titleLarge.nunito(600), titleMedium = m3.titleMedium.nunito(600), titleSmall = m3.titleSmall.nunito(600),
        bodyLarge = m3.bodyLarge.nunito(400), bodyMedium = m3.bodyMedium.nunito(400), bodySmall = m3.bodySmall.nunito(400),
        labelLarge = m3.labelLarge.nunito(600), labelMedium = m3.labelMedium.nunito(600), labelSmall = m3.labelSmall.nunito(600),
    )
}
