package shelftap.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import shelftap.android.R
import shelftap.android.ui.icons.LucideIcon
import shelftap.android.ui.icons.LucideIcons
import shelftap.android.ui.theme.Spacing

// The colours of the board `Splash`: the same in the light and dark themes (the starting window is the same teal).
private val TEAL = Color(0xFF2F4A44)
private val TEAL_SOFT = Color(0xFF3D5F57)
private val GOLD = Color(0xFFC9A66B)
private const val GOLD_ALPHA = 0.55f
private const val TILE_FILL = 0.22f
private const val TILE_EDGE = 0.5f
private const val TEXT_ALPHA = 0.85f
private const val TRACK_ALPHA = 0.2f

/**
 * The start screen, built to the board `Splash`: teal with two soft blobs, the book on a glass tile, the name and a line, and under them a bar
 * and "checking your session". It shows while the stored session is looked at (the system's starting window gives way to it after a moment).
 * The board's blur behind the blobs is drawn as soft-edged gradients, which cost no blur pass at start.
 */
@Composable
fun StartupScreen() {
    Box(
        Modifier.fillMaxSize().background(TEAL).drawBehind {
            val d = density
            // Left blob (a 320 dp circle) and right blob (300 dp), at the board's places on a 390 x 844 page.
            drawCircle(Brush.radialGradient(listOf(TEAL_SOFT, TEAL_SOFT.copy(alpha = 0f)), Offset(70f * d, 280f * d), 190f * d), 190f * d, Offset(70f * d, 280f * d))
            val right = Offset(size.width - 40f * d, 570f * d)
            drawCircle(Brush.radialGradient(listOf(GOLD.copy(alpha = GOLD_ALPHA), GOLD.copy(alpha = 0f)), right, 180f * d), 180f * d, right)
        },
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(TOP_SHARE))
            val tile = RoundedCornerShape(TILE_RADIUS)
            Box(
                Modifier.size(TILE).clip(tile).background(Color.White.copy(alpha = TILE_FILL)).border(androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = TILE_EDGE)), tile),
                contentAlignment = Alignment.Center,
            ) { LucideIcon(LucideIcons.MockupCatalog, tint = Color.White, size = 44.dp) }
            Spacer(Modifier.height(Spacing.l + Spacing.xs))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(Spacing.xs + Spacing.xs / 2))
            Text(
                stringResource(R.string.splash_tagline), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = TEXT_ALPHA),
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = Spacing.xl),
            )
            Spacer(Modifier.weight(BOTTOM_SHARE))
            Column(
                Modifier.navigationBarsPadding().padding(horizontal = 60.dp).padding(bottom = 120.dp).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                LinearProgressIndicator(
                    Modifier.fillMaxWidth().height(4.dp).clip(CircleShape), color = Color.White, trackColor = Color.White.copy(alpha = TRACK_ALPHA),
                )
                Text(stringResource(R.string.splash_checking), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = TEXT_ALPHA), textAlign = TextAlign.Center)
            }
        }
    }
}

private val TILE = 96.dp
private val TILE_RADIUS = 30.dp
private const val TOP_SHARE = 0.36f // the tile's top is about 36% down the page, as in the board (300 of 844)
private const val BOTTOM_SHARE = 0.64f
