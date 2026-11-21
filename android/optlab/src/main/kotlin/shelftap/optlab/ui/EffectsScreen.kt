package shelftap.optlab.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import shelftap.optlab.Probe
import shelftap.optlab.RecompositionProbe
import shelftap.optlab.data.Book
import shelftap.optlab.data.BookGenerator

/** P08: decorative-effect tax in list items. Same lazy list, keys and data in both variants; only the row differs. */
@Composable
fun EffectsScreen(variantA: Boolean, n: Int) {
    val books = androidx.compose.runtime.remember { BookGenerator.all(n) }
    LaunchedEffect(Unit) { Probe.gauge("fx.perItem", if (variantA) EFFECTS_PER_ITEM_A else 0L) }
    FullyDrawn()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(books, key = { it.id }, contentType = { "book" }) { if (variantA) EffectRowA(it) else FlatRow(it) }
    }
}

/** shadow + blur + gradient + infinite animation. */
private const val EFFECTS_PER_ITEM_A = 4L

/** A: the "AI look": big shadow, glass blur, gradient card, pulsing alpha animated in composition. */
@Composable
private fun EffectRowA(book: Book) {
    RecompositionProbe("rc.row")
    val pulse = rememberInfiniteTransition(label = "pulse")
    val a by pulse.animateFloat(0.6f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "alpha")
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth()
            .shadow(18.dp, shape, ambientColor = Color(0xFF6A5ACD), spotColor = Color(0xFFFF4081))
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF7F5AF0), Color(0xFFFF8906), Color(0xFFE53170))))
            .alpha(a)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(72.dp, 108.dp).blur(8.dp)
                .background(Brush.verticalGradient(listOf(coverColor(book.coverIdx), Color.White.copy(alpha = 0.3f)))),
        )
        Spacer(Modifier.width(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(book.title, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 2)
            Text("${book.author} · ${book.year}", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** B: same content, flat surface with a hairline border. */
@Composable
private fun FlatRow(book: Book) {
    RecompositionProbe("rc.row")
    Row(
        Modifier.fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(72.dp, 108.dp).background(coverColor(book.coverIdx)))
        Spacer(Modifier.width(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
            Text("${book.author} · ${book.year}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
