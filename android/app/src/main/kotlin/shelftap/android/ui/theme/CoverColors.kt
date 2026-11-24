package shelftap.android.ui.theme

import androidx.compose.ui.graphics.Color

/** Background and text colour pairs of the text covers (from the mockup); each pair clears 4.5:1. */
private val coverPalette = listOf(
    Color(0xFFE7DCC6) to Color(0xFF2F4A44),
    Color(0xFFB5452B) to Color(0xFFFFFFFF),
    Color(0xFFD8C9A8) to Color(0xFF3E3A36),
    Color(0xFF3E3A36) to Color(0xFFFFFFFF),
    Color(0xFFC9A66B) to Color(0xFF1C1B1A),
    Color(0xFF7A2E1E) to Color(0xFFFFFFFF),
)

/** The same title always gets the same colours. */
fun coverColors(title: String): Pair<Color, Color> = coverPalette[Math.floorMod(title.hashCode(), coverPalette.size)]

/** The banner of the catalog home: a dark teal field with a darker scrim at the foot, white text on both (checked in the mockup). */
val bannerBackground = Color(0xFF2F4A44)
val bannerScrim = Color(0xFF141E1B)

/** The solid red of the sign-out confirmation: white text on it clears 4.5:1 in both themes (a fixed colour in the mockup). */
val dangerSolid = Color(0xFF7A3420)
