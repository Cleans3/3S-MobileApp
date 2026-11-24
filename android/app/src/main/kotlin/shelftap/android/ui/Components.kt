package shelftap.android.ui

import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import shelftap.android.R
import shelftap.android.ui.icons.LucideMorphs
import shelftap.android.ui.icons.MorphIcon
import shelftap.android.ui.theme.EffectTier
import shelftap.android.ui.theme.LocalEffectTier
import shelftap.android.ui.theme.Motion
import shelftap.android.ui.theme.pressScale
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
            PrimaryButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
        }
    }
}

/** The dimmest point of the skeleton pulse. */
private const val SKELETON_LOW_ALPHA = 0.55f

/**
 * Placeholder rows with the same cover size as the real rows, so the list does not jump when data arrives. Static
 * (no shimmer): it costs no animation frames on a low-end phone. TalkBack reads one "Loading" for the whole block.
 */
@Composable
fun SkeletonList(rows: Int = 6, modifier: Modifier = Modifier) {
    val loading = stringResource(R.string.state_loading)
    val block = MaterialTheme.colorScheme.surfaceVariant
    val shape = MaterialTheme.shapes.extraSmall
    // The blocks breathe (1.2 s) on the full and medium tiers; still on the light tier. The loop ends when the skeleton leaves the screen.
    val tier = LocalEffectTier.current
    val pulse = if (tier == EffectTier.LIGHT) null else rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 1f, targetValue = SKELETON_LOW_ALPHA,
        animationSpec = infiniteRepeatable(tween(Motion.PULSE_MS, easing = LinearEasing), RepeatMode.Reverse), label = "pulse",
    )
    Column(
        modifier.graphicsLayer { alpha = pulse?.value ?: 1f }.clearAndSetSemantics { contentDescription = loading; liveRegion = LiveRegionMode.Polite },
    ) {
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

/** A password field with a show/hide eye. The eye morphs to the crossed eye; the shown state is never saved. */
@Composable
fun PasswordField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf(false) }
    val description = stringResource(if (shown) R.string.password_hide else R.string.password_show)
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) }, singleLine = true,
        visualTransformation = if (shown) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = modifier,
        trailingIcon = {
            IconButton(onClick = { shown = !shown }, modifier = Modifier.semantics { contentDescription = description }) {
                MorphIcon(LucideMorphs.EyeToEyeOff, toEnd = shown)
            }
        },
    )
}

/** The magnifier in a search field; once there is text it morphs to a cross that clears the field. */
@Composable
fun SearchTrailingIcon(hasText: Boolean, onClear: () -> Unit) {
    val description = stringResource(R.string.search_clear)
    Box(
        Modifier.size(Spacing.touch).then(
            if (hasText) Modifier.semantics { contentDescription = description }.pressable(CircleShape, onClear, shrink = false) else Modifier.clearAndSetSemantics {},
        ),
        contentAlignment = Alignment.Center,
    ) { MorphIcon(LucideMorphs.SearchToX, toEnd = hasText) }
}

/** The filled button of the app: shrinks a little while pressed (plan section 6.5). */
@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    Button(onClick, modifier.pressScale(source), enabled, interactionSource = source, contentPadding = contentPadding, content = content)
}

/** The outlined button of the app, same press feedback. */
@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    OutlinedButton(onClick, modifier.pressScale(source), enabled, interactionSource = source, content = content)
}
