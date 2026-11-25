package shelftap.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import shelftap.android.R
import shelftap.android.api.ApiException
import shelftap.android.api.ErrorCode
import shelftap.android.api.HistoryItem
import shelftap.android.data.Repository
import shelftap.android.ui.icons.LucideIcon
import shelftap.android.ui.icons.LucideIcons
import shelftap.android.ui.theme.GlassSize
import shelftap.android.ui.theme.Radius
import shelftap.android.ui.theme.Spacing
import shelftap.android.ui.theme.itemMotion
import shelftap.android.ui.theme.rememberRiseIn
import shelftap.android.ui.theme.riseIn
import shelftap.android.ui.theme.shelfTap

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

private const val ENTRY_FILL = 0.8f
private const val STEP_FILL = 0.72f
private val THUMB_WIDTH = 44.dp
private val THUMB_HEIGHT = 60.dp
private val LOCK_BADGE = 56.dp

/**
 * The reading history, built to the board `History`: a glass card per borrowed book (a small cover, the title, when it was borrowed and
 * returned), then a row that leads to the Account screen, where saving the history is switched on or off. A card session sees the
 * password card instead (the server answers STEP_UP_REQUIRED). Not shown, because the API has no such data: the author.
 */
@Composable
fun HistoryScreen(onAccount: () -> Unit = {}) {
    val vm = appViewModel { HistoryViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    var askPassword by rememberSaveable { mutableStateOf(false) }

    if (state.needsPassword) {
        Box(Modifier.fillMaxSize().padding(pagePadding()), contentAlignment = Alignment.Center) { StepUpCard(onLogin = { askPassword = true }) }
    } else {
        LoadView(state.items, onRetry = vm::load, loading = { SkeletonList(modifier = Modifier.fillMaxSize()) }) { items ->
            if (items.isEmpty()) {
                EmptyState(R.string.history_empty)
            } else {
                val rise = rememberRiseIn()
                LazyColumn(
                    Modifier.fillMaxSize(), state = rememberPreparedListState(), contentPadding = pagePadding(GlassSize.page, Spacing.s),
                    verticalArrangement = Arrangement.spacedBy(Spacing.m - Spacing.xs / 2),
                ) {
                    itemsIndexed(items) { index, h -> HistoryEntry(h, Modifier.then(itemMotion()).riseIn(index, rise)) }
                    item { ManageRow(onAccount) }
                }
            }
        }
    }
    if (askPassword) StepUpDialog(onDismiss = { askPassword = false }, onDone = { askPassword = false; vm.load() })
}

@Composable
private fun HistoryEntry(h: HistoryItem, modifier: Modifier = Modifier) {
    val c = MaterialTheme.shelfTap
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.card)).background(c.glass.copy(alpha = ENTRY_FILL))
            .padding(horizontal = Spacing.l, vertical = Spacing.m + Spacing.xs / 2),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m + Spacing.xs / 2),
    ) {
        TextCover(h.title, Modifier.size(THUMB_WIDTH, THUMB_HEIGHT), RoundedCornerShape(Spacing.s))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs / 2)) {
            Text(h.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
            Text(
                h.returnedAt?.let { stringResource(R.string.history_returned, formatDay(h.outAt), formatDay(it)) }
                    ?: stringResource(R.string.history_not_returned, formatDay(h.outAt)),
                style = MaterialTheme.typography.bodySmall, color = c.ink2,
            )
        }
    }
}

/** The row at the end: it says where saving the history is switched, and takes the member there. */
@Composable
private fun ManageRow(onAccount: () -> Unit) {
    val c = MaterialTheme.shelfTap
    Row(
        Modifier.fillMaxWidth().pressable(RoundedCornerShape(Radius.card), onAccount, row = true).background(c.surface).padding(horizontal = Spacing.l, vertical = Spacing.m + Spacing.xs / 2),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.history_manage), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        LucideIcon(LucideIcons.ChevronRight, tint = c.ink, size = GlassSize.arrowIcon)
    }
}

/** What a card login sees instead of the history: why a password is needed, and the button to give it. */
@Composable
internal fun StepUpCard(onLogin: () -> Unit) {
    val c = MaterialTheme.shelfTap
    Column(
        Modifier.fillMaxWidth().padding(horizontal = GlassSize.page).clip(RoundedCornerShape(Radius.result)).background(c.glass.copy(alpha = STEP_FILL)).padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.m + Spacing.xs / 2),
    ) {
        Box(Modifier.size(LOCK_BADGE).clip(CircleShape).background(c.surface), contentAlignment = Alignment.Center) {
            LucideIcon(LucideIcons.Lock, tint = c.ink, size = Spacing.xl + Spacing.xs / 2)
        }
        Text(stringResource(R.string.history_step_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.history_needs_password), style = MaterialTheme.typography.bodyMedium, color = c.ink2)
        PillAction(stringResource(R.string.history_login_password), onClick = onLogin, container = c.primary, ink = c.onPrimary)
    }
}
