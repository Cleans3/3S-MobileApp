package shelftap.android.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import shelftap.android.R
import shelftap.android.ui.theme.CoverSize
import shelftap.android.ui.theme.Spacing

/** Nothing to show, and what to do about it (plan §5.4: the empty state of every list). */
@Composable
fun EmptyState(@StringRes title: Int, @StringRes hint: Int? = null, modifier: Modifier = Modifier.fillMaxSize()) {
    Box(modifier.padding(Spacing.xl), contentAlignment = Alignment.Center) {
        Column(
            Modifier.semantics(mergeDescendants = true) {},
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            hint?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) }
        }
    }
}

/**
 * A failed load with a way forward. [offline] (no connection) is told apart from a server answer, because the next
 * step differs: check the network, or try again later. The message is announced when it appears.
 */
@Composable
fun ErrorState(message: UiText, offline: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier.fillMaxSize()) {
    Box(modifier.padding(Spacing.xl), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Column(
                Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                if (offline) Text(stringResource(R.string.state_offline_title), style = MaterialTheme.typography.titleMedium)
                Text(message.asString(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
            Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
        }
    }
}

/**
 * Placeholder rows with the same cover size as the real rows, so the list does not jump when data arrives. Static
 * (no shimmer): it costs no animation frames on a low-end phone. TalkBack reads one "Loading" for the whole block.
 */
@Composable
fun SkeletonList(rows: Int = 6, modifier: Modifier = Modifier) {
    val loading = stringResource(R.string.state_loading)
    val block = MaterialTheme.colorScheme.surfaceVariant
    val shape = MaterialTheme.shapes.extraSmall
    Column(modifier.clearAndSetSemantics { contentDescription = loading; liveRegion = LiveRegionMode.Polite }) {
        repeat(rows) {
            Row(Modifier.fillMaxWidth().padding(Spacing.l), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                Box(Modifier.size(width = CoverSize.rowWidth, height = CoverSize.rowHeight).background(block, shape))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Box(Modifier.fillMaxWidth(0.7f).height(Spacing.l).background(block, shape))
                    Box(Modifier.fillMaxWidth(0.45f).height(Spacing.m).background(block, shape))
                    Box(Modifier.fillMaxWidth(0.3f).height(Spacing.m).background(block, shape))
                }
            }
        }
    }
}
