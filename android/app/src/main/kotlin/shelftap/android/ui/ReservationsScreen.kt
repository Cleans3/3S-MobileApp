package shelftap.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
import shelftap.android.api.Reservation
import shelftap.android.api.ReservationStatus
import shelftap.android.data.Repository
import shelftap.android.ui.theme.GlassSize
import shelftap.android.ui.theme.Radius
import shelftap.android.ui.theme.Spacing
import shelftap.android.ui.theme.dangerSolid
import shelftap.android.ui.theme.itemMotion
import shelftap.android.ui.theme.pressScale
import shelftap.android.ui.theme.readyFill
import shelftap.android.ui.theme.rememberRiseIn
import shelftap.android.ui.theme.riseIn
import shelftap.android.ui.theme.shelfTap

class ReservationsViewModel(private val repo: Repository) : ViewModel() {
    private val _state = MutableStateFlow(ListState<Reservation>())
    val state: StateFlow<ListState<Reservation>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = ListState()
        viewModelScope.launch {
            val list = try { Load.Ready(repo.reservations()) } catch (e: ApiException) { e.toFailed() }
            _state.update { it.copy(list = list) }
        }
    }

    /** Cancels after the member agreed in the card; the id goes to `confirmed` only once the server answered. */
    fun cancel(reservation: Reservation) {
        _state.update { it.copy(busyId = reservation.reservationId, error = null) }
        viewModelScope.launch {
            try {
                repo.cancelReservation(reservation.reservationId)
                _state.update { s ->
                    val list = (s.list as? Load.Ready)?.value?.map {
                        if (it.reservationId == reservation.reservationId) it.copy(status = ReservationStatus.CANCELLED) else it
                    }
                    s.copy(list = list?.let { Load.Ready(it) } ?: s.list, busyId = null, confirmed = s.confirmed + reservation.reservationId)
                }
            } catch (e: ApiException) {
                _state.update { it.copy(busyId = null, error = e.userMessage()) }
            }
        }
    }
}

private const val CARD_FILL = 0.8f
private const val CHIP_FILL_READY = 0.2f
private const val CHIP_EDGE_READY = 0.4f
private val CHIP_PAD_H = 10.dp
private val CHIP_PAD_V = 4.dp
private const val OUTLINE_ALPHA = 0.2f

/**
 * The member's reservations, built to the board `Reservations`: a book that is ready to collect gets a deep teal card, the others a
 * glass card with a "Waiting" chip; cancelling asks first inside the card ("Keep it" / "Cancel reservation") and, once the server
 * answered, the card gives way to a one-line notice. Not shown, because the API has no such data: the author, the day the book is held
 * until ("giữ đến ..."), and a promise to notify.
 */
@Composable
fun ReservationsScreen() {
    val vm = appViewModel { ReservationsViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    var asking by rememberSaveable { mutableStateOf<String?>(null) }
    LoadView(state.list, onRetry = vm::load, loading = { SkeletonList(modifier = Modifier.fillMaxSize()) }) { reservations ->
        val rise = rememberRiseIn()
        LazyColumn(
            Modifier.fillMaxSize(), state = rememberPreparedListState(), contentPadding = pagePadding(GlassSize.page, Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.m + Spacing.xs),
        ) {
            item { ErrorText(state.error) }
            if (reservations.isEmpty()) item { EmptyState(R.string.reservations_empty, R.string.reservations_empty_hint, Modifier.fillParentMaxSize()) }
            itemsIndexed(reservations, key = { _, it -> it.reservationId }) { index, r ->
                val mod = Modifier.then(itemMotion()).riseIn(index, rise)
                if (r.reservationId in state.confirmed) {
                    CancelledNotice(r.title, mod)
                } else {
                    ReservationCard(
                        r, asking = asking == r.reservationId, busy = state.busyId == r.reservationId, canTap = state.busyId == null,
                        onAsk = { asking = r.reservationId }, onKeep = { asking = null },
                        onCancel = { vm.cancel(r); asking = null },
                        modifier = mod,
                    )
                }
            }
        }
    }
}

@Composable
private fun CancelledNotice(title: String, modifier: Modifier = Modifier) {
    val c = MaterialTheme.shelfTap
    Text(
        stringResource(R.string.reservations_cancelled_notice, title), style = MaterialTheme.typography.bodyMedium, color = c.ink2,
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.stat)).background(c.surface).padding(horizontal = Spacing.l, vertical = Spacing.m + Spacing.xs / 2)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun ReservationCard(
    r: Reservation, asking: Boolean, busy: Boolean, canTap: Boolean, onAsk: () -> Unit, onKeep: () -> Unit, onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MaterialTheme.shelfTap
    val ready = r.status == ReservationStatus.READY
    val ink = if (ready) Color.White else c.ink
    val sub = if (ready) Color.White.copy(alpha = 0.9f) else c.ink2
    val open = r.status == ReservationStatus.WAITING || r.status == ReservationStatus.READY
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.banner)).background(if (ready) readyFill else c.glass.copy(alpha = CARD_FILL)).padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m + Spacing.xs / 2)) {
            TextCover(r.title, Modifier.size(GlassSize.loanCoverWidth, GlassSize.loanCoverHeight), RoundedCornerShape(Spacing.m))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs + Spacing.xs / 2)) {
                StatusChip(
                    stringResource(
                        when (r.status) {
                            ReservationStatus.READY -> R.string.res_chip_ready
                            ReservationStatus.WAITING -> R.string.res_chip_waiting
                            ReservationStatus.FULFILLED -> R.string.res_fulfilled
                            ReservationStatus.CANCELLED -> R.string.res_cancelled
                            ReservationStatus.EXPIRED -> R.string.res_expired
                        },
                    ),
                    ready,
                )
                Text(r.title, style = MaterialTheme.typography.titleMedium, color = ink, maxLines = 3)
                val line = when (r.status) {
                    ReservationStatus.WAITING -> stringResource(R.string.res_waiting, formatDay(r.placedAt))
                    ReservationStatus.READY -> r.readyAt?.let { stringResource(R.string.res_ready_since, formatDay(it)) } ?: stringResource(R.string.res_ready)
                    else -> null
                }
                if (line != null) Text(line, style = MaterialTheme.typography.bodyMedium, color = sub)
            }
        }
        if (open) {
            val cancelFor = stringResource(R.string.reservations_cancel_for, r.title)
            if (asking) {
                // Asked inside the card, not in a dialog: "Keep it" is the first and the calmer choice.
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.tile + Spacing.xs)).background(MaterialTheme.colorScheme.background).padding(Spacing.m + Spacing.xs / 2),
                    verticalArrangement = Arrangement.spacedBy(Spacing.m - Spacing.xs / 2),
                ) {
                    Text(stringResource(R.string.reservations_cancel_ask, r.title), style = MaterialTheme.typography.titleSmall, color = c.ink)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        OutlinePill(stringResource(R.string.reservations_keep), onKeep, Modifier.weight(1f), container = c.surface, ink = c.ink)
                        SolidPill(stringResource(R.string.reservations_cancel), onCancel, Modifier.weight(1f).semantics { contentDescription = cancelFor }, enabled = canTap)
                    }
                }
            } else {
                OutlinePill(
                    stringResource(if (busy) R.string.busy_dots else R.string.reservations_cancel), onAsk,
                    Modifier.semantics { contentDescription = cancelFor }, enabled = canTap, ink = ink, edge = ink.copy(alpha = OUTLINE_ALPHA),
                )
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, onTeal: Boolean) {
    val c = MaterialTheme.shelfTap
    val shape = CircleShape
    Text(
        label, style = MaterialTheme.typography.labelMedium, color = if (onTeal) Color.White else c.ink2,
        modifier = Modifier.clip(shape)
            .then(
                if (onTeal) Modifier.background(Color.White.copy(alpha = CHIP_FILL_READY)).border(BorderStroke(Radius.hairline, Color.White.copy(alpha = CHIP_EDGE_READY)), shape)
                else Modifier.background(MaterialTheme.colorScheme.background),
            )
            .padding(horizontal = CHIP_PAD_H, vertical = CHIP_PAD_V),
    )
}

/** A pill with an outline (44 dp tall), as the board's "Hủy đặt trước" and "Giữ lại". */
@Composable
private fun OutlinePill(
    label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    ink: Color = MaterialTheme.shelfTap.ink, edge: Color = MaterialTheme.shelfTap.ink.copy(alpha = OUTLINE_ALPHA), container: Color = Color.Transparent,
) {
    val source = remember { MutableInteractionSource() }
    Row(
        modifier.height(GlassSize.renewButton).pressScale(source).clip(CircleShape).background(container)
            .border(BorderStroke(Radius.hairline, edge), CircleShape)
            .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.l),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) { Text(label, style = MaterialTheme.typography.titleSmall, color = ink) }
}

/** A solid red pill (44 dp tall) for the step that cannot be taken back. */
@Composable
private fun SolidPill(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val source = remember { MutableInteractionSource() }
    Row(
        modifier.height(GlassSize.renewButton).pressScale(source).clip(CircleShape).background(dangerSolid)
            .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.l),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) { Text(label, style = MaterialTheme.typography.titleSmall, color = Color.White) }
}
