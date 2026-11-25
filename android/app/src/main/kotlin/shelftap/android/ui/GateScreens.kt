package shelftap.android.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import shelftap.android.R
import shelftap.android.api.ApiException
import shelftap.android.data.Repository
import shelftap.android.security.BiometricLock
import shelftap.android.ui.icons.LucideIcon
import shelftap.android.ui.icons.LucideIcons
import shelftap.android.ui.theme.GateSize
import shelftap.android.ui.theme.LoginSize
import shelftap.android.ui.theme.Radius
import shelftap.android.ui.theme.ShelfTapText
import shelftap.android.ui.theme.Spacing
import shelftap.android.ui.theme.okFill
import shelftap.android.ui.theme.shelfTap

private const val CARD_FILL = 0.92f // the board's 0.62 to 0.7 sits on a blur; with none the card must stay readable
private const val CARD_EDGE = 0.9f
private const val CHIP_FILL = 0.7f
private const val DARK_BLOB_ALPHA = 0.35f
private val BLOB_TAN = Color(0xFFD7C39A)
private val BLOB_SAGE = Color(0xFFB9C7BF)

/**
 * Two soft patches of colour behind the top of a screen (the boards' tan and sage circles, blurred there): drawn as radial gradients that
 * fade out at their edge, which costs no blur. Dimmer on a dark page. Decorative.
 */
@Composable
private fun SoftBackdrop(modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val scale = if (dark) DARK_BLOB_ALPHA else 1f
    Canvas(modifier.clearAndSetSemantics {}) {
        fun blob(color: Color, centre: Offset, radius: Float) =
            drawCircle(Brush.radialGradient(0f to color.copy(alpha = scale), 0.6f to color.copy(alpha = scale * 0.7f), 1f to Color.Transparent, center = centre, radius = radius), radius, centre)
        val d = density
        blob(BLOB_TAN, Offset((-80f + 160f) * d, (-60f + 160f) * d), 230f * d)
        blob(BLOB_SAGE, Offset(size.width + (90f - 130f) * d, (80f + 130f) * d), 190f * d)
    }
}

/** The glass card the three screens share: rounded 34 dp, an edge, a soft shadow. */
@Composable
private fun GateCard(modifier: Modifier = Modifier, fill: Float = CARD_FILL, content: @Composable () -> Unit) {
    val c = MaterialTheme.shelfTap
    val shape = RoundedCornerShape(Radius.sheet)
    Column(
        modifier.fillMaxWidth().shadow(Spacing.xl, shape, ambientColor = c.ink.copy(alpha = 0.16f), spotColor = c.ink.copy(alpha = 0.16f))
            .clip(shape).background(c.glass.copy(alpha = fill)).border(BorderStroke(Radius.hairline, c.edge.copy(alpha = CARD_EDGE)), shape)
            .padding(horizontal = Spacing.l + Spacing.xs + Spacing.xs / 2, vertical = Spacing.xl + Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.m + Spacing.xs / 2),
    ) { content() }
}

/** The round white badge with an icon on top of a status card. */
@Composable
private fun GateBadge(icon: String) {
    val c = MaterialTheme.shelfTap
    Box(Modifier.size(GateSize.badge).shadow(Spacing.m, CircleShape).clip(CircleShape).background(c.surface).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        LucideIcon(icon, tint = c.ink, size = GateSize.badgeIcon)
    }
}

class PasswordViewModel(private val repo: Repository) : ViewModel() {
    data class UiState(val busy: Boolean = false, val error: UiText? = null, val done: Boolean = false)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun change(current: String, new: String) {
        if (_state.value.busy) return
        _state.value = UiState(busy = true)
        viewModelScope.launch {
            _state.value = try {
                repo.changePassword(current, new)
                UiState(done = true)
            } catch (e: ApiException) {
                UiState(error = e.userMessage())
            }
        }
    }
}

/** The server's rule for a new password: at least this many characters (`MeController.PasswordRequest`). */
private const val MIN_PASSWORD = 10

/**
 * Changing the password, built to the board `ChangePassword`: [forced] (the first sign-in with the password the library issued) says so in a
 * chip and a headline; the three fields sit on a glass card with the rules ticking off as they hold, and the save button wakes when all do.
 * The server checks them again. Changing it ends the member's other sessions. Passwords are kept in memory only.
 */
@Composable
fun ChangePasswordScreen(forced: Boolean, onDone: () -> Unit) {
    val vm = appViewModel { PasswordViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val session = LocalContainer.current.session
    val scope = rememberCoroutineScope()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    ChangePasswordContent(forced, state.busy, state.error, onSave = vm::change, onLogout = { scope.launch { session.logout() } })
}

/** What the password screen draws and the three fields it keeps (in memory only), apart from the model that saves: [onSave] gets current and new. */
@Composable
internal fun ChangePasswordContent(forced: Boolean, busy: Boolean, error: UiText?, onSave: (String, String) -> Unit, onLogout: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    val c = MaterialTheme.shelfTap

    val longEnough = new.length >= MIN_PASSWORD
    val differs = new.isNotEmpty() && new != current
    val same = new.isNotEmpty() && new == again
    val valid = current.isNotEmpty() && longEnough && differs && same

    Box(Modifier.fillMaxSize()) {
        SoftBackdrop(Modifier.fillMaxWidth().height(GateSize.backdrop))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pagePadding()).padding(horizontal = Spacing.l)) {
            Column(Modifier.padding(horizontal = Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.m - Spacing.xs / 2)) {
                if (forced) {
                    Text(
                        stringResource(R.string.pw_first_login), style = MaterialTheme.typography.labelMedium, color = c.ink2,
                        modifier = Modifier.clip(CircleShape).background(c.glass.copy(alpha = CHIP_FILL)).padding(horizontal = Spacing.m, vertical = Spacing.xs + Spacing.xs / 2),
                    )
                }
                Text(stringResource(if (forced) R.string.pw_forced_title else R.string.pw_title), style = ShelfTapText.detailTitle)
                Text(stringResource(if (forced) R.string.pw_forced_body else R.string.pw_body), style = MaterialTheme.typography.bodyMedium, color = c.ink2)
            }
            Spacer(Modifier.height(Spacing.xl + Spacing.m))
            GateCard {
                LabeledField(stringResource(if (forced) R.string.pw_current_issued else R.string.pw_current), current, { current = it }, keyboardType = KeyboardType.Password, password = true, enabled = !busy)
                LabeledField(stringResource(R.string.pw_new), new, { new = it }, keyboardType = KeyboardType.Password, password = true, enabled = !busy, placeholder = stringResource(R.string.pw_new_hint))
                LabeledField(stringResource(R.string.pw_again), again, { again = it }, keyboardType = KeyboardType.Password, password = true, enabled = !busy, placeholder = stringResource(R.string.pw_again_hint))
                Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    RuleRow(stringResource(R.string.pw_rule_length, MIN_PASSWORD), longEnough)
                    RuleRow(stringResource(R.string.pw_rule_differs), differs)
                    RuleRow(stringResource(R.string.pw_rule_same), same)
                }
                ErrorText(error)
                PillAction(
                    stringResource(if (busy) R.string.pw_saving else R.string.pw_save), onClick = { onSave(current, new) },
                    container = c.primary, ink = c.onPrimary, enabled = !busy && valid, height = LoginSize.button,
                )
            }
            if (forced) TextAction(stringResource(R.string.action_logout), onClick = onLogout, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

/** One rule of a new password: a dot that turns into a green tick when the rule holds (said in words too, not only by colour). */
@Composable
private fun RuleRow(label: String, ok: Boolean) {
    val c = MaterialTheme.shelfTap
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m - Spacing.xs / 2)) {
        Box(Modifier.size(Spacing.l + Spacing.xs).clip(CircleShape).background(if (ok) okFill else c.ink.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            if (ok) LucideIcon(LucideIcons.Check, tint = Color.White, size = Spacing.m)
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.ink2)
    }
}

/** The blocks of a home screen that show, blurred, behind the lock (decorative): a search pill, a banner and three cards, from the board. */
@Composable
private fun LockedBackdrop(modifier: Modifier = Modifier) {
    val c = MaterialTheme.shelfTap
    Column(modifier.clearAndSetSemantics {}.padding(horizontal = Spacing.xl - Spacing.xs, vertical = Spacing.xl * 5), verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
        Box(Modifier.fillMaxWidth().height(52.dp).clip(CircleShape).background(c.surface))
        Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(Radius.banner)).background(Color(0xFF2F4A44)))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            for (color in listOf(Color(0xFFC9A66B), Color(0xFFB5452B), Color(0xFF3E3A36))) {
                Box(Modifier.size(132.dp, 196.dp).clip(RoundedCornerShape(Radius.card)).background(color))
            }
        }
    }
}

/**
 * The lock, built to the board `Lock`: the app's home blocks behind a veil, and a glass card that says it is locked, with the button
 * that asks for the biometric again (it is asked once on its own), a way to sign out, and what a changed fingerprint does.
 */
@Composable
fun LockScreen() {
    val session = LocalContainer.current.session
    val activity = LocalActivity.current as FragmentActivity
    val scope = rememberCoroutineScope()
    val title = stringResource(R.string.lock_title)
    val cancelLabel = stringResource(R.string.action_cancel)
    val unlock: () -> Unit = {
        scope.launch {
            session.unlock { cipher ->
                BiometricLock.authenticate(activity, cipher, title = title, cancelLabel = cancelLabel)
            }
        }
    }
    LaunchedEffect(Unit) { unlock() }
    LockContent(onUnlock = unlock, onLogout = { scope.launch { session.logout() } })
}

@Composable
internal fun LockContent(onUnlock: () -> Unit, onLogout: () -> Unit) {
    val c = MaterialTheme.shelfTap
    Box(Modifier.fillMaxSize()) {
        LockedBackdrop(Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = LOCK_VEIL)))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pagePadding()).padding(horizontal = Spacing.xl), verticalArrangement = Arrangement.Center) {
            GateCard(fill = LOCK_CARD_FILL) {
                GateBadge(LucideIcons.Lock)
                Text(stringResource(R.string.lock_locked_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.lock_body), style = MaterialTheme.typography.bodyMedium, color = c.ink2)
                PillAction(stringResource(R.string.lock_unlock), onClick = onUnlock, container = c.primary, ink = c.onPrimary, height = LoginSize.button)
                TextAction(stringResource(R.string.action_logout), onClick = onLogout, modifier = Modifier.align(Alignment.CenterHorizontally), ink = c.ink2)
            }
            Text(
                stringResource(R.string.lock_footnote), style = MaterialTheme.typography.bodySmall, color = c.ink2, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m, vertical = Spacing.xl),
            )
        }
    }
}

private const val LOCK_CARD_FILL = 0.98f // the blocks behind the lock must not show through the card
private const val LOCK_VEIL = 0.72f // the board blurs the blocks and veils them at 0.35; with no blur they are hidden more

/**
 * Shown when the server could not be reached at start-up: the cause, a retry that says what it is doing and how many times it has failed,
 * and a way to sign out. Not shown, because the app does not have them yet: searching the catalog offline, and the promise to retry by itself.
 */
@Composable
fun OfflineScreen() {
    val session = LocalContainer.current.session
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var tries by remember { mutableIntStateOf(0) }
    OfflineContent(
        busy, tries,
        onRetry = {
            busy = true
            scope.launch {
                try { session.restore() } finally { busy = false; tries += 1 } // if it worked this screen is gone; if not, the count says so
            }
        },
        onLogout = { scope.launch { session.logout() } },
    )
}

@Composable
internal fun OfflineContent(busy: Boolean, tries: Int, onRetry: () -> Unit, onLogout: () -> Unit) {
    val c = MaterialTheme.shelfTap
    Box(Modifier.fillMaxSize()) {
        SoftBackdrop(Modifier.fillMaxWidth().height(GateSize.backdrop))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pagePadding()).padding(horizontal = Spacing.l), verticalArrangement = Arrangement.Center) {
            GateCard {
                GateBadge(LucideIcons.CircleAlert)
                Text(stringResource(R.string.offline_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.offline_body), style = MaterialTheme.typography.bodyMedium, color = c.ink2)
                if (tries > 0) {
                    Text(
                        stringResource(R.string.offline_still, tries), style = MaterialTheme.typography.labelLarge, color = c.danger,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                PillAction(
                    stringResource(if (busy) R.string.offline_trying else R.string.action_retry), onClick = onRetry, enabled = !busy, height = LoginSize.button,
                    container = c.primary, ink = c.onPrimary,
                )
                TextAction(stringResource(R.string.action_logout), onClick = onLogout, modifier = Modifier.align(Alignment.CenterHorizontally), ink = c.ink2)
            }
        }
    }
}
