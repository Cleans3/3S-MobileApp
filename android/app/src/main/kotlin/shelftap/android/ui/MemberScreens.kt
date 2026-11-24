package shelftap.android.ui

import android.nfc.Tag
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import shelftap.android.ui.theme.EffectTier
import shelftap.android.ui.theme.GlassSize
import shelftap.android.ui.theme.LocalEffectTier
import shelftap.android.ui.theme.NfcSize
import shelftap.android.ui.theme.Radius
import shelftap.android.ui.theme.dangerSolid
import shelftap.android.ui.theme.okFill
import shelftap.android.ui.theme.pressScale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import shelftap.android.R
import shelftap.android.api.ApiException
import shelftap.android.api.ErrorCode
import shelftap.android.api.HistoryItem
import shelftap.android.api.Loan
import shelftap.android.api.Reservation
import shelftap.android.api.ReservationStatus
import shelftap.android.data.Repository
import shelftap.android.nfc.TagReader
import shelftap.android.ui.icons.LucideIcon
import shelftap.android.ui.icons.LucideIcons
import shelftap.android.ui.icons.LucideMorphs
import shelftap.android.ui.icons.MorphIcon
import shelftap.android.ui.theme.IconSize
import shelftap.android.ui.theme.Spacing
import shelftap.android.ui.theme.itemMotion
import shelftap.android.ui.theme.rememberRiseIn
import shelftap.android.ui.theme.riseIn
import shelftap.android.ui.theme.shelfTap
import java.time.Instant

/** A list screen's state: the loaded list plus the outcome of the last row action. */
data class ListState<T>(
    val list: Load<List<T>> = Load.Loading, val busyId: String? = null, val error: UiText? = null,
    /** Ids whose action the server confirmed (a loan renewed): shown with the confirmed icon. */
    val confirmed: Set<String> = emptySet(),
)

class CheckoutViewModel(private val repo: Repository) : ViewModel() {
    sealed interface UiState {
        data object Idle : UiState
        data object Working : UiState
        data class Done(val loan: Loan) : UiState
        /** [unpaidFine]: the server refused because of a fine, so the way on is the loans screen, where it is shown. */
        data class Failed(val message: UiText, val unpaidFine: Boolean = false) : UiState
    }

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Reader-mode callback. Reads the tag ID and asks the server for a loan; the server alone decides. No tag is written. */
    fun tagTapped(tag: Tag) {
        if (_state.value == UiState.Working) return
        val tagId = TagReader.bookTagId(tag)
        if (tagId == null) {
            _state.value = UiState.Failed(uiText(R.string.err_not_book_tag))
            return
        }
        _state.value = UiState.Working
        viewModelScope.launch {
            _state.value = try {
                UiState.Done(repo.checkout(tagId))
            } catch (e: ApiException) {
                UiState.Failed(e.userMessage(), unpaidFine = e is ApiException.Http && e.code == ErrorCode.UNPAID_FINE)
            }
        }
    }

    fun reset() {
        if (_state.value != UiState.Working) _state.value = UiState.Idle
    }
}

/**
 * Self-checkout, built to the board `Checkout`: the disc that waits for a tag ([NfcStage]), a card while the request is on its way
 * (the tag was read, the library is deciding: nothing is "borrowed" yet), a card with a tick once the server confirmed the loan, and a
 * red card with the reason when it refused (a fine leads to the loans screen). [onLoans] opens it.
 */
@Composable
fun CheckoutScreen(onLoans: () -> Unit = {}) {
    val vm = appViewModel { CheckoutViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(state is CheckoutViewModel.UiState.Working) {
        if (state is CheckoutViewModel.UiState.Working) haptics.performHapticFeedback(HapticFeedbackType.ContextClick) // the tag was read
    }
    NfcGate {
        NfcReaderEffect(TagReader.BOOK_FLAGS) { vm.tagTapped(it) }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pagePadding()).padding(horizontal = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val s = state) {
                CheckoutViewModel.UiState.Idle -> {
                    NfcStage()
                    Text(stringResource(R.string.checkout_hold), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                }
                CheckoutViewModel.UiState.Working -> SendingCard()
                is CheckoutViewModel.UiState.Done -> DoneCard(s.loan, onAgain = vm::reset, onLoans = onLoans)
                is CheckoutViewModel.UiState.Failed -> FailedCard(s, onAgain = vm::reset, onLoans = onLoans)
            }
        }
    }
}

/** A card that says how the checkout stands: glass (0.72 while sending, 0.8 when done) or the red tint of a refusal. */
@Composable
private fun ResultCard(container: Color, modifier: Modifier = Modifier, shake: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val tier = LocalEffectTier.current
    val wobble = remember { Animatable(if (shake && tier != EffectTier.LIGHT) 0f else 1f) }
    LaunchedEffect(shake) { if (shake && tier != EffectTier.LIGHT) wobble.animateTo(1f, tween(RESULT_SHAKE_MS, easing = LinearEasing)) }
    val distance = with(LocalDensity.current) { RESULT_SHAKE_DP.dp.toPx() }
    Column(
        modifier.fillMaxWidth().graphicsLayer { translationX = sin(wobble.value * 2 * PI.toFloat() * 2) * (1f - wobble.value) * distance }
            .clip(RoundedCornerShape(Radius.result)).background(container).padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.m + Spacing.xs / 2),
        content = content,
    )
}

private const val RESULT_SHAKE_MS = 360
private const val RESULT_SHAKE_DP = 6
private const val SENDING_FILL = 0.72f
private const val DONE_FILL = 0.8f

@Composable
internal fun SendingCard() {
    val c = MaterialTheme.shelfTap
    ResultCard(c.glass.copy(alpha = SENDING_FILL)) {
        // The tag is read; the library has not answered: the second step is still open (a ring), not a tick.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Box(Modifier.size(NfcSize.step).clip(CircleShape).background(okFill), contentAlignment = Alignment.Center) {
                LucideIcon(LucideIcons.Check, tint = Color.White, size = Spacing.m + Spacing.xs / 2)
            }
            Text(stringResource(R.string.checkout_read), style = MaterialTheme.typography.titleSmall)
        }
        Row(
            Modifier.semantics { liveRegion = LiveRegionMode.Polite }, verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Box(Modifier.size(NfcSize.step).border(BorderStroke(Spacing.xs / 2, c.ink), CircleShape))
            Text(stringResource(R.string.checkout_sending), style = MaterialTheme.typography.titleSmall)
        }
        Text(stringResource(R.string.checkout_sending_note), style = MaterialTheme.typography.bodySmall, color = c.ink2)
    }
}

@Composable
internal fun DoneCard(loan: Loan, onAgain: () -> Unit, onLoans: () -> Unit) {
    val c = MaterialTheme.shelfTap
    val days = daysToDue(loan.dueAt)
    ResultCard(c.glass.copy(alpha = DONE_FILL)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.m + Spacing.xs / 2)) {
            DoneBadge()
            Text(
                stringResource(R.string.checkout_borrowed, loan.title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Text(
                if (days != null && days > 0) pluralStringResource(R.plurals.checkout_due_days, days, formatDay(loan.dueAt), days)
                else stringResource(R.string.checkout_due, formatDay(loan.dueAt)),
                style = MaterialTheme.typography.bodyLarge, color = c.ink2, textAlign = TextAlign.Center,
            )
        }
        PillAction(stringResource(R.string.checkout_again), onClick = onAgain, container = c.primary, ink = c.onPrimary)
        TextAction(stringResource(R.string.checkout_view_loans), onClick = onLoans, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
internal fun FailedCard(s: CheckoutViewModel.UiState.Failed, onAgain: () -> Unit, onLoans: () -> Unit) {
    val c = MaterialTheme.shelfTap
    ResultCard(c.dangerBg, shake = true) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m - Spacing.xs / 2)) {
            LucideIcon(LucideIcons.CircleAlert, tint = c.danger, size = Spacing.xl - Spacing.xs / 2)
            Text(stringResource(R.string.checkout_failed_title), style = MaterialTheme.typography.titleMedium, color = c.danger)
        }
        Text(
            s.message.asString(), style = MaterialTheme.typography.bodyMedium, color = c.ink2,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (s.unpaidFine) {
            PillAction(stringResource(R.string.checkout_view_fines), onClick = onLoans, container = dangerSolid, ink = Color.White)
            TextAction(stringResource(R.string.checkout_try_other), onClick = onAgain, modifier = Modifier.align(Alignment.CenterHorizontally), ink = c.ink2)
        } else {
            PillAction(stringResource(R.string.action_retry), onClick = onAgain, container = dangerSolid, ink = Color.White)
        }
    }
}

class HistoryViewModel(private val repo: Repository) : ViewModel() {
    data class UiState(val items: Load<List<HistoryItem>> = Load.Loading, val needsPassword: Boolean = false)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState()
        viewModelScope.launch {
            _state.value = try {
                UiState(Load.Ready(repo.history()))
            } catch (e: ApiException) {
                // A card session may not read history (403 STEP_UP_REQUIRED): offer the password login instead of an error.
                if (e is ApiException.Http && e.code == ErrorCode.STEP_UP_REQUIRED) UiState(needsPassword = true)
                else UiState(e.toFailed())
            }
        }
    }
}

@Composable
fun HistoryScreen() {
    val vm = appViewModel { HistoryViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    var askPassword by rememberSaveable { mutableStateOf(false) }

    if (state.needsPassword) {
        Centered {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                Text(stringResource(R.string.history_needs_password))
                PrimaryButton(onClick = { askPassword = true }) { Text(stringResource(R.string.history_login_password)) }
            }
        }
    } else {
        LoadView(state.items, onRetry = vm::load, loading = { SkeletonList(modifier = Modifier.fillMaxSize()) }) { items ->
            if (items.isEmpty()) {
                EmptyState(R.string.history_empty)
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = pagePadding()) {
                    items(items) { h ->
                        Column(Modifier.fillMaxWidth().padding(Spacing.l)) {
                            Text(h.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                h.returnedAt?.let { stringResource(R.string.history_returned, formatDate(h.outAt), formatDate(it)) }
                                    ?: stringResource(R.string.history_not_returned, formatDate(h.outAt)),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
    if (askPassword) StepUpDialog(onDismiss = { askPassword = false }, onDone = { askPassword = false; vm.load() })
}
