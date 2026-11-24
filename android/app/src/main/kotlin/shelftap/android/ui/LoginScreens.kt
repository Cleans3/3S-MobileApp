package shelftap.android.ui

import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import shelftap.android.R
import shelftap.android.nfc.TagReader
import shelftap.android.session.LogoutReason
import shelftap.android.ui.icons.LucideIcon
import shelftap.android.ui.icons.LucideIcons
import shelftap.android.ui.theme.EffectTier
import shelftap.android.ui.theme.LocalEffectTier
import shelftap.android.ui.theme.LoginSize
import shelftap.android.ui.theme.Motion
import shelftap.android.ui.theme.Radius
import shelftap.android.ui.theme.ShelfTapText
import shelftap.android.ui.theme.Spacing
import shelftap.android.ui.theme.okFill
import shelftap.android.ui.theme.pressScale
import shelftap.android.ui.theme.shelfTap

@StringRes
private fun LogoutReason.message() = when (this) {
    LogoutReason.SESSION_EXPIRED -> R.string.logout_session_expired
    LogoutReason.TOKEN_REUSED -> R.string.logout_token_reused
    LogoutReason.INACTIVITY -> R.string.logout_inactivity
    LogoutReason.LOCK_KEY_INVALIDATED -> R.string.logout_lock_key_invalidated
}

private const val FORM_FILL = 0.92f // the board's 0.68 sits on a blur; with none the form must stay readable over the books
private const val FORM_EDGE = 0.9f
private const val OR_RULE_ALPHA = 0.12f
private const val CARD_BUTTON_FILL = 0.85f
private const val FADE_FROM = 0.5f
private const val TOP_VEIL = 0.85f
private const val TOP_VEIL_END = 0.3f // of the backdrop's height: about the bar

// The books on the shelves of the login: widths and heights in dp and a palette, from the board (so every run draws the same shelves).
private val BOOK_COLORS = listOf(0xFF8E5A3C, 0xFF2F4A44, 0xFFC9A66B, 0xFF6B6F7A, 0xFFB5452B, 0xFF3E3A36, 0xFFD8C9A8, 0xFF556B5A, 0xFFA3633F, 0xFF41506B).map { Color(it) }
private val BOOK_WIDTHS = intArrayOf(22, 30, 18, 26, 34, 20, 28, 24, 16, 30, 22, 26)
private val BOOK_HEIGHTS = intArrayOf(130, 112, 140, 100, 126, 144, 108, 132, 120, 104, 138, 116)
private val PLANK = Color(0xFF8A7461)

/** Two shelves of books behind the top of the login, fading into the page; decorative (nothing in it is read out). */
@Composable
private fun ShelfBackdrop(modifier: Modifier = Modifier) {
    val c = MaterialTheme.shelfTap
    val page = MaterialTheme.colorScheme.background
    Canvas(modifier.clearAndSetSemantics {}) {
        drawRect(c.bg2)
        val dp = density
        var top = 30f * dp
        for (shelf in 0..1) {
            val rowHeight = 150f * dp
            var x = 14f * dp
            for (i in 0 until 12) {
                val w = BOOK_WIDTHS[(i + shelf * 3) % 12] * dp
                val h = BOOK_HEIGHTS[(i * 5 + shelf) % 12] * dp
                drawRoundRect(BOOK_COLORS[(i * 3 + shelf * 7) % 10], Offset(x, top + rowHeight - h), Size(w, h), CornerRadius(2f * dp))
                x += w + 4f * dp
            }
            drawRect(PLANK, Offset(0f, top + rowHeight), Size(size.width, 9f * dp))
            top += rowHeight + 9f * dp
        }
        drawRect(Brush.verticalGradient(FADE_FROM to Color.Transparent, 1f to page))
        // The top bar sits over the books: a veil of the page colour keeps its title readable.
        drawRect(Brush.verticalGradient(0f to page.copy(alpha = TOP_VEIL), TOP_VEIL_END to Color.Transparent))
    }
}

/**
 * The login, built to the board `Login`: shelves of books at the top, a glass form over them (email, password with the eye, the
 * login button, "or", the card button) and a line about how to get an account. [reason] says why the member was signed out.
 * The password lives in memory only: it is not saved with the screen's state. (The board's blur behind the form is not drawn.)
 */
@Composable
fun LoginScreen(reason: LogoutReason?, onCard: () -> Unit) {
    val vm = appViewModel { AuthViewModel(it.session) }
    val state by vm.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") } // never saved to instance state
    val c = MaterialTheme.shelfTap
    val formShape = RoundedCornerShape(Radius.result)

    Box(Modifier.fillMaxSize()) {
        ShelfBackdrop(Modifier.fillMaxWidth().height(LoginSize.backdrop))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pagePadding(0.dp, 0.dp))) {
            Spacer(Modifier.height(LoginSize.formTop))
            Column(
                Modifier.padding(horizontal = Spacing.l).fillMaxWidth()
                    .shadow(Spacing.xl, formShape, ambientColor = c.ink.copy(alpha = 0.18f), spotColor = c.ink.copy(alpha = 0.18f))
                    .clip(formShape).background(c.glass.copy(alpha = FORM_FILL)).border(BorderStroke(Radius.hairline, c.edge.copy(alpha = FORM_EDGE)), formShape)
                    .padding(start = Spacing.l + Spacing.xs, end = Spacing.l + Spacing.xs, top = Spacing.xl, bottom = Spacing.l + Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.l),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(stringResource(R.string.login_action), style = ShelfTapText.detailTitle)
                    Text(stringResource(R.string.login_subtitle), style = MaterialTheme.typography.bodyMedium, color = c.ink2)
                }
                IntegrityBanner()
                reason?.let { Text(stringResource(it.message()), style = MaterialTheme.typography.bodyMedium, color = c.danger) }
                LabeledField(stringResource(R.string.login_email), email, { email = it }, keyboardType = KeyboardType.Email, enabled = !state.busy)
                LabeledField(stringResource(R.string.login_password), password, { password = it }, keyboardType = KeyboardType.Password, password = true, enabled = !state.busy)
                ErrorText(state.error)
                PillAction(
                    stringResource(if (state.busy) R.string.login_busy else R.string.login_action), onClick = { vm.login(email, password) },
                    container = c.primary, ink = c.onPrimary, enabled = !state.busy && email.isNotBlank() && password.isNotEmpty(), height = LoginSize.button,
                    description = stringResource(R.string.login_with_password), // the heading above is also "Login": two controls named alike confuse TalkBack
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m - Spacing.xs / 2)) {
                    Box(Modifier.weight(1f).height(Radius.hairline).background(c.ink.copy(alpha = OR_RULE_ALPHA)))
                    Text(stringResource(R.string.login_or), style = MaterialTheme.typography.labelMedium, color = c.ink3)
                    Box(Modifier.weight(1f).height(Radius.hairline).background(c.ink.copy(alpha = OR_RULE_ALPHA)))
                }
                CardButton(onCard, enabled = !state.busy)
            }
            Text(
                stringResource(R.string.login_footer), style = MaterialTheme.typography.bodySmall, color = c.ink2, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.xl, bottom = Spacing.xl * 2),
            )
        }
    }
}

/** The glass button of the form that leads to the card login. */
@Composable
private fun CardButton(onClick: () -> Unit, enabled: Boolean) {
    val c = MaterialTheme.shelfTap
    val source = remember { MutableInteractionSource() }
    Row(
        Modifier.fillMaxWidth().height(LoginSize.button).pressScale(source).clip(CircleShape).background(c.glass.copy(alpha = CARD_BUTTON_FILL))
            .border(BorderStroke(Radius.hairline, c.ink.copy(alpha = OR_RULE_ALPHA)), CircleShape)
            .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(Spacing.m - Spacing.xs / 2, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
    ) {
        LucideIcon(LucideIcons.CreditCard, tint = c.ink, size = Spacing.l + Spacing.xs)
        Text(stringResource(R.string.login_with_card), style = MaterialTheme.typography.titleSmall, color = c.ink)
    }
}

private const val STAGE_RING_CYCLE_MS = 1800
private const val STAGE_BREATH_MS = 2400
private const val STAGE_PING_END = 1.35f
private const val STAGE_PING_ALPHA = 0.45f

private val CARD_TEAL = Color(0xFF2F4A44)
private val CARD_CHIP = Color(0xFFC9A66B)

/** The drawing of a library card tilted on two glass discs, with rings spreading from it while the phone waits; stronger discs while a card is being checked. */
@Composable
private fun CardStage(active: Boolean) {
    val c = MaterialTheme.shelfTap
    val tier = LocalEffectTier.current
    val loop = if (tier != EffectTier.LIGHT) rememberInfiniteTransition(label = "card") else null
    val phase = loop?.animateFloat(0f, 1f, infiniteRepeatable(tween(STAGE_RING_CYCLE_MS, easing = LinearEasing)), label = "ping")
    val breath = loop?.animateFloat(1f, 1.04f, infiniteRepeatable(tween(STAGE_BREATH_MS / 2, easing = Motion.decelerate), RepeatMode.Reverse), label = "breath")
    Box(Modifier.size(LoginSize.stage).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxSize().clip(CircleShape).background(c.glass.copy(alpha = if (active) 0.55f else 0.35f))
                .border(BorderStroke(Radius.hairline, c.edge.copy(alpha = FORM_EDGE)), CircleShape),
        )
        Box(
            Modifier.size(LoginSize.disc).clip(CircleShape).background(c.glass.copy(alpha = if (active) 0.8f else 0.55f))
                .border(BorderStroke(Radius.hairline, c.edge.copy(alpha = 0.95f)), CircleShape),
        )
        if (phase != null) {
            Canvas(Modifier.size(LoginSize.disc)) {
                val edge = size.minDimension / 2
                for (k in 0..1) {
                    val t = (phase.value + k * 0.5f) % 1f
                    drawCircle(CARD_TEAL.copy(alpha = STAGE_PING_ALPHA * (1f - t)), radius = edge * (1f + (STAGE_PING_END - 1f) * t), style = Stroke(2f * density))
                }
            }
        }
        Box(
            Modifier.graphicsLayer { val s = breath?.value ?: 1f; scaleX = s; scaleY = s; rotationZ = -8f }
                .shadow(Spacing.xl, RoundedCornerShape(Radius.tile), ambientColor = CARD_TEAL, spotColor = CARD_TEAL)
                .size(LoginSize.cardWidth, LoginSize.cardHeight).clip(RoundedCornerShape(Radius.tile)).background(CARD_TEAL)
                .padding(Spacing.m),
        ) {
            Text("ShelfTap", color = Color.White, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.align(Alignment.TopStart))
            Box(Modifier.align(Alignment.BottomStart).size(30.dp, 22.dp).clip(RoundedCornerShape(5.dp)).background(CARD_CHIP))
            Text(stringResource(R.string.card_illustration_name), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.BottomEnd))
        }
    }
}

/**
 * The login with a library card, built to the board `CardLogin`: how to hold the card, the drawing, three steps that follow what happens
 * (the card is read, the library checks it, signed in: a successful login leaves this screen), and a line saying why no PIN is asked.
 */
@Composable
fun CardLoginScreen(onBack: () -> Unit) {
    val vm = appViewModel { AuthViewModel(it.session) }
    val state by vm.state.collectAsStateWithLifecycle()
    NfcGate {
        NfcReaderEffect(TagReader.CARD_FLAGS) { vm.cardTapped(it) }
        CardLoginContent(busy = state.busy, error = state.error, onBack = onBack)
    }
}

/** What the card login shows, apart from reading the card (so it can be drawn where there is no NFC). */
@Composable
internal fun CardLoginContent(busy: Boolean, error: UiText?, onBack: () -> Unit) {
    val c = MaterialTheme.shelfTap
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pagePadding()).padding(horizontal = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs + Spacing.xs / 2)) {
            Text(stringResource(R.string.card_title), style = ShelfTapText.detailTitle)
            Text(stringResource(R.string.card_subtitle), style = MaterialTheme.typography.bodyMedium, color = c.ink2)
        }
        Spacer(Modifier.height(Spacing.xl))
        CardStage(active = busy)
        Spacer(Modifier.height(Spacing.xl))
        // The steps follow what is happening: waiting for the card, then the library checking it.
        val steps = listOf(R.string.card_step_read, R.string.card_step_verify, R.string.card_step_done)
        val current = if (busy) 1 else 0
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.xs).clip(RoundedCornerShape(Radius.banner)).background(c.glass.copy(alpha = 0.7f))
                .border(BorderStroke(Radius.hairline, c.edge.copy(alpha = FORM_EDGE)), RoundedCornerShape(Radius.banner))
                .padding(horizontal = Spacing.l + Spacing.xs / 2, vertical = Spacing.l).semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            steps.forEachIndexed { i, label -> StepRow(stringResource(label), done = i < current, current = i == current && i < steps.lastIndex) }
        }
        ErrorText(error, Modifier.padding(top = Spacing.m))
        Spacer(Modifier.height(Spacing.l))
        TextAction(stringResource(R.string.card_use_password), onClick = onBack)
        Text(
            stringResource(R.string.card_footnote), style = MaterialTheme.typography.bodySmall, color = c.ink2, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.m),
        )
    }
}

@Composable
private fun StepRow(label: String, done: Boolean, current: Boolean) {
    val c = MaterialTheme.shelfTap
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Box(
            Modifier.size(Spacing.xl).clip(CircleShape)
                .background(if (done) okFill else if (current) c.surface else c.ink.copy(alpha = 0.08f))
                .then(if (current) Modifier.border(BorderStroke(Spacing.xs / 2, c.ink), CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) { if (done) LucideIcon(LucideIcons.Check, tint = Color.White, size = Spacing.m + Spacing.xs / 2) }
        Text(
            if (current) "$label…" else label, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (current) FontWeight.Bold else FontWeight.Medium),
            color = if (done || current) c.ink else c.ink3,
        )
    }
}
