package shelftap.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import shelftap.android.R
import shelftap.android.api.ApiException
import shelftap.android.api.Fine
import shelftap.android.api.Loan
import shelftap.android.data.Repository
import shelftap.android.ui.icons.LucideIcon
import shelftap.android.ui.icons.LucideIcons
import shelftap.android.ui.icons.LucideMorphs
import shelftap.android.ui.icons.MorphIcon
import shelftap.android.ui.theme.GlassSize
import shelftap.android.ui.theme.Radius
import shelftap.android.ui.theme.Spacing
import shelftap.android.ui.theme.busyFill
import shelftap.android.ui.theme.itemMotion
import shelftap.android.ui.theme.okFill
import shelftap.android.ui.theme.overdueEdge
import shelftap.android.ui.theme.pressScale
import shelftap.android.ui.theme.rememberRiseIn
import shelftap.android.ui.theme.riseIn
import shelftap.android.ui.theme.shelfTap

/** Whole calendar days from today to the due date: 0 today, negative once past. Null when the date cannot be read. */
internal fun daysToDue(dueAt: String, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): Int? =
    try {
        ChronoUnit.DAYS.between(now.atZone(zone).toLocalDate(), Instant.parse(dueAt).atZone(zone).toLocalDate()).toInt()
    } catch (_: DateTimeParseException) {
        null
    }

/** True when the due date (ISO instant from the server) is in the past. An unreadable date is not overdue. */
internal fun isOverdue(dueAt: String, now: Instant = Instant.now()): Boolean =
    try { Instant.parse(dueAt).isBefore(now) } catch (_: DateTimeParseException) { false }

/** What the member still owes for one loan: the sum of its unpaid fines. */
internal fun unpaidFor(fines: List<Fine>, loanId: String): Long = fines.filter { it.loanId == loanId && it.paidAt == null }.sumOf { it.amountVnd }

/** An amount of đồng with the locale's grouping ("2.000 ₫" in Vietnamese). */
internal fun formatVnd(amount: Long, locale: Locale = Locale.getDefault()): String = NumberFormat.getIntegerInstance(locale).format(amount) + " ₫"

private val DAY_MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private fun formatDay(iso: String): String =
    try { Instant.parse(iso).atZone(ZoneId.systemDefault()).format(DAY_MONTH_YEAR) } catch (_: DateTimeParseException) { iso }

/** The card is the glass of the mockup (0.8 over the page). */
private const val LOAN_FILL = 0.8f
private val OVERDUE_BORDER = 2.dp

class LoansViewModel(private val repo: Repository) : ViewModel() {
    private val _state = MutableStateFlow(ListState<Loan>())
    val state: StateFlow<ListState<Loan>> = _state.asStateFlow()

    /** The member's fines, or null while they load or when they could not be read (the loans do not wait for them). */
    private val _fines = MutableStateFlow<List<Fine>?>(null)
    val fines: StateFlow<List<Fine>?> = _fines.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = ListState()
        _fines.value = null
        viewModelScope.launch {
            val list = try { Load.Ready(repo.loans()) } catch (e: ApiException) { e.toFailed() }
            _state.update { it.copy(list = list) }
        }
        viewModelScope.launch {
            _fines.value = try { repo.fines() } catch (_: ApiException) { null }
        }
    }

    fun renew(loan: Loan) {
        _state.update { it.copy(busyId = loan.loanId, error = null) }
        viewModelScope.launch {
            try {
                val renewed = repo.renew(loan.loanId)
                _state.update { s ->
                    val list = (s.list as? Load.Ready)?.value?.map { if (it.loanId == renewed.loanId) renewed else it }
                    s.copy(list = list?.let { Load.Ready(it) } ?: s.list, busyId = null, confirmed = s.confirmed + renewed.loanId)
                }
            } catch (e: ApiException) {
                _state.update { it.copy(busyId = null, error = e.userMessage()) }
            }
        }
    }
}

/**
 * The member's loans, built to the board `Loans`: two figures (books on loan, unpaid fines), then one glass card per book with its
 * cover, due date and a renew button; an overdue book gets an orange edge, an icon and its fine, and no button.
 * Not shown, because the API has no such data: the limit of loans at once ("/ tối đa 5"), the author.
 */
@Composable
fun LoansScreen() {
    val vm = appViewModel { LoansViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val fines by vm.fines.collectAsStateWithLifecycle()
    LoadView(state.list, onRetry = vm::load, loading = { SkeletonList(modifier = Modifier.fillMaxSize()) }) { loans ->
        val rise = rememberRiseIn()
        val owed = fines?.filter { it.paidAt == null }?.sumOf { it.amountVnd }
        LazyColumn(
            Modifier.fillMaxSize(), state = rememberPreparedListState(), contentPadding = pagePadding(GlassSize.page, Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            item { ErrorText(state.error) }
            if (loans.isEmpty() && (owed ?: 0L) == 0L) {
                item { EmptyState(R.string.loans_empty, R.string.loans_empty_hint, Modifier.fillParentMaxSize()) }
            } else {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.m - Spacing.xs / 2)) {
                        StatTile(stringResource(R.string.loans_count_label), loans.size.toString(), Modifier.weight(1f))
                        if (owed != null) {
                            val c = MaterialTheme.shelfTap
                            val owing = owed > 0
                            StatTile(
                                stringResource(R.string.loans_fines_label), formatVnd(owed), Modifier.weight(1f),
                                container = if (owing) c.dangerBg else c.surface, labelColor = if (owing) c.danger else c.ink3, valueColor = if (owing) c.danger else c.ink,
                            )
                        }
                    }
                }
            }
            itemsIndexed(loans, key = { _, it -> it.loanId }) { index, loan ->
                LoanCard(
                    loan, fine = fines?.let { unpaidFor(it, loan.loanId) } ?: 0L, renewing = state.busyId == loan.loanId,
                    renewed = loan.loanId in state.confirmed, canTap = state.busyId == null, onRenew = { vm.renew(loan) },
                    modifier = Modifier.then(itemMotion()).riseIn(index, rise),
                )
            }
        }
    }
}

@Composable
private fun LoanCard(loan: Loan, fine: Long, renewing: Boolean, renewed: Boolean, canTap: Boolean, onRenew: () -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.shelfTap
    val overdue = isOverdue(loan.dueAt)
    val days = daysToDue(loan.dueAt)
    val shape = RoundedCornerShape(Radius.panel)
    Row(
        modifier.fillMaxWidth().clip(shape).background(c.glass.copy(alpha = LOAN_FILL))
            .then(if (overdue) Modifier.border(BorderStroke(OVERDUE_BORDER, overdueEdge), shape) else Modifier)
            .padding(Spacing.m + Spacing.xs / 2),
        horizontalArrangement = Arrangement.spacedBy(Spacing.m + Spacing.xs / 2),
    ) {
        TextCover(loan.title, Modifier.size(GlassSize.loanCoverWidth, GlassSize.loanCoverHeight), RoundedCornerShape(Spacing.m))
        Column(Modifier.weight(1f).heightIn(min = GlassSize.loanCoverHeight), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(loan.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
            if (overdue) {
                // Overdue is said in words and with an icon, not only by the colour of the edge.
                val late = days?.let { -it }?.takeIf { it > 0 }
                val words = if (late != null) pluralStringResource(R.plurals.loans_overdue_days, late, late) else stringResource(R.string.loans_overdue)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs + Spacing.xs / 2)) {
                    LucideIcon(LucideIcons.CircleAlert, tint = c.danger, size = Spacing.l)
                    Text(
                        if (fine > 0) stringResource(R.string.loans_overdue_fine, words, formatVnd(fine)) else words,
                        style = MaterialTheme.typography.labelLarge, color = c.danger,
                    )
                }
            } else {
                val due = formatDay(loan.dueAt)
                Text(
                    when {
                        renewed -> stringResource(R.string.loans_due_new, due)
                        days == null -> stringResource(R.string.loans_due_plain, due)
                        days == 0 -> stringResource(R.string.loans_due_today, due)
                        else -> stringResource(R.string.loans_due_left, due, days)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (loan.renewable || renewed) {
                Spacer(Modifier.weight(1f))
                RenewButton(loan.title, renewing, renewed, enabled = canTap && !renewed, onClick = onRenew)
            } else {
                Text(
                    stringResource(if (overdue) R.string.loans_overdue_note else R.string.err_renewal_not_allowed),
                    style = MaterialTheme.typography.bodySmall, color = c.ink2,
                )
            }
        }
    }
}

/** The renew pill: dark, then grey while the request is on its way, then green only after the server confirmed (the clock turns into a ticked calendar). */
@Composable
private fun RenewButton(title: String, renewing: Boolean, renewed: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.shelfTap
    val source = remember { MutableInteractionSource() }
    val (fill, ink) = when {
        renewed -> okFill to Color.White
        renewing -> busyFill to Color.White
        else -> c.primary to c.onPrimary
    }
    val description = stringResource(if (renewed) R.string.loans_renewed_for else R.string.loans_renew_for, title)
    Row(
        Modifier.height(GlassSize.renewButton).pressScale(source).clip(CircleShape).background(fill)
            .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.l + Spacing.xs / 2)
            // Two "Renew" buttons read the same otherwise; the outcome is announced when it changes.
            .semantics { contentDescription = description; liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically,
    ) {
        MorphIcon(LucideMorphs.ClockToCalendarCheck, toEnd = renewed, tint = ink)
        Text(
            stringResource(when { renewed -> R.string.loans_renewed; renewing -> R.string.busy_dots; else -> R.string.loans_renew }),
            style = MaterialTheme.typography.titleSmall, color = ink,
        )
    }
}
