package shelftap.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import shelftap.android.R
import shelftap.android.recommend.BookInfo
import shelftap.android.recommend.Reason
import shelftap.android.recommend.Recommendation
import shelftap.android.recommend.RecommendationService
import shelftap.android.ui.theme.CoverSize
import shelftap.android.ui.theme.GlassSize
import shelftap.android.ui.theme.Radius
import shelftap.android.ui.theme.Spacing
import shelftap.android.ui.theme.itemMotion
import shelftap.android.ui.theme.rememberRiseIn
import shelftap.android.ui.theme.riseIn
import shelftap.android.ui.theme.shelfTap

private const val CARD_FILL = 0.8f
private const val NOTE_FILL = 0.72f

/** What the screen draws once the suggestions are known: the list (possibly empty) and what it is built from. */
private data class RecommendContent(val items: List<Recommendation>, val basis: RecommendationService.Basis)

/**
 * "Suggestions for you", from the board `Recommend`: a line saying what the suggestions are built from, then one glass card per book with the
 * reason in words, the copies left and "Not interested". A card login sees a way to enter the password for better suggestions (the same
 * step-up as the reading history). With nothing to go on, a short explanation and two ways forward. On a wide window the list sits beside
 * "Why this suggestion", the chosen book in its own panel.
 *
 * The suggestions are worked out on this phone by [RecommendationService]; nothing leaves it.
 */
@Composable
fun RecommendScreen(onOpen: (String) -> Unit, onCatalog: () -> Unit, onAccount: () -> Unit) {
    val service = LocalContainer.current.recommender
    val state by service.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var askPassword by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(service) { service.ensureLoaded() }

    val load: Load<RecommendContent> = when (val s = state) {
        RecommendationService.State.Idle, RecommendationService.State.Loading -> Load.Loading
        is RecommendationService.State.Failed -> s.error.toFailed()
        is RecommendationService.State.Ready -> Load.Ready(RecommendContent(s.items, s.basis))
        is RecommendationService.State.Empty -> Load.Ready(RecommendContent(emptyList(), s.basis))
    }
    LoadView(load, onRetry = { scope.launch { service.refresh() } }, loading = { SkeletonList(modifier = Modifier.fillMaxWidth()) }) { content ->
        val selected = content.items.firstOrNull { it.book.titleId == selectedId } ?: content.items.firstOrNull()
        val listContent: @Composable (Boolean) -> Unit = { twoPane ->
            RecommendList(
                content, twoPane, selected?.book?.titleId,
                onOpen = onOpen, onSelect = { selectedId = it }, onDismiss = service::dismiss,
                onPassword = { askPassword = true }, onCatalog = onCatalog, onAccount = onAccount,
            )
        }
        if (content.items.isEmpty()) {
            listContent(false)
        } else {
            TwoPane(
                detailTitle = stringResource(R.string.recommend_why_title), list = listContent,
                detail = { selected?.let { RecommendDetail(it, onOpen = { onOpen(it.book.titleId) }, onDismiss = { service.dismiss(it.book.titleId) }) } },
            )
        }
    }
    if (askPassword) StepUpDialog(onDismiss = { askPassword = false }, onDone = { askPassword = false; scope.launch { service.refresh() } })
}

@Composable
private fun RecommendList(
    content: RecommendContent, twoPane: Boolean, selectedId: String?, onOpen: (String) -> Unit, onSelect: (String) -> Unit, onDismiss: (String) -> Unit,
    onPassword: () -> Unit, onCatalog: () -> Unit, onAccount: () -> Unit,
) {
    val c = MaterialTheme.shelfTap
    val rise = rememberRiseIn()
    LazyColumn(
        Modifier.fillMaxWidth(), state = rememberPreparedListState(), contentPadding = pagePadding(GlassSize.page, Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        item {
            Text(
                stringResource(
                    when (content.basis) {
                        RecommendationService.Basis.FULL -> R.string.recommend_sub_full
                        RecommendationService.Basis.CARD -> R.string.recommend_sub_card
                        RecommendationService.Basis.NO_HISTORY -> R.string.recommend_sub_off
                    },
                ),
                style = MaterialTheme.typography.bodyMedium, color = c.ink2, modifier = Modifier.padding(horizontal = Spacing.xs),
            )
        }
        if (content.basis == RecommendationService.Basis.CARD) {
            item {
                Row(
                    Modifier.fillMaxWidth().pressable(RoundedCornerShape(Radius.stat), onPassword, row = true).background(c.surface)
                        .padding(horizontal = Spacing.l, vertical = Spacing.m),
                ) { Text(stringResource(R.string.recommend_more_accurate), style = MaterialTheme.typography.labelLarge) }
            }
        }
        if (content.items.isEmpty()) {
            item { RecommendEmpty(onCatalog, onAccount) }
        } else {
            itemsIndexed(content.items, key = { _, it -> it.book.titleId }) { index, r ->
                val mod = Modifier.then(itemMotion()).riseIn(index, rise)
                if (twoPane) RecommendRow(r, selected = r.book.titleId == selectedId, onClick = { onSelect(r.book.titleId) }, modifier = mod)
                else RecommendCard(r, onOpen = { onOpen(r.book.titleId) }, onDismiss = { onDismiss(r.book.titleId) }, modifier = mod)
            }
            item {
                Text(
                    stringResource(R.string.recommend_note), style = MaterialTheme.typography.bodySmall, color = c.ink3,
                    modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xs),
                )
            }
        }
    }
}

/** Nothing to go on: what that means in a sentence, then the two things the member can do about it. */
@Composable
private fun RecommendEmpty(onCatalog: () -> Unit, onAccount: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Text(stringResource(R.string.recommend_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.recommend_empty_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.shelfTap.ink2)
        PrimaryButton(onClick = onCatalog, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.recommend_by_subject)) }
        SecondaryButton(onClick = onAccount, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.recommend_history_settings)) }
    }
}

@Composable
private fun reasonText(reason: Reason): String = when (reason) {
    is Reason.SameAuthor -> stringResource(R.string.recommend_reason_author, reason.author)
    is Reason.SameSubject -> stringResource(R.string.recommend_reason_subject, reason.subject)
}

@Composable
private fun statusText(book: BookInfo): String =
    if (book.copiesAvailable > 0) stringResource(R.string.catalog_available, book.copiesAvailable) else stringResource(R.string.recommend_status_on_loan)

/** One suggested book as a card: cover, title, author, the reason, the copies left, and "Not interested". The card opens the book. */
@Composable
private fun RecommendCard(r: Recommendation, onOpen: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.shelfTap
    val shape = RoundedCornerShape(Radius.banner)
    val dismissLabel = stringResource(R.string.recommend_dismiss_for, r.book.title)
    Column(modifier.fillMaxWidth().clip(shape).background(c.glass.copy(alpha = CARD_FILL))) {
        Row(
            Modifier.fillMaxWidth().pressable(shape, onOpen, role = null, row = true).padding(start = Spacing.m, end = Spacing.m, top = Spacing.m),
            horizontalArrangement = Arrangement.spacedBy(Spacing.m + Spacing.xs / 2),
        ) {
            Cover(r.book.coverUrl, r.book.title, Modifier.size(CoverSize.rowWidth, CoverSize.rowHeight), RoundedCornerShape(Spacing.s))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs / 2)) {
                Text(r.book.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                if (r.book.authors.isNotEmpty()) Text(r.book.authors.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = c.ink2, maxLines = 1)
                Text(reasonText(r.reason), style = MaterialTheme.typography.bodySmall, color = c.warn, fontWeight = FontWeight.Bold)
                Text(statusText(r.book), style = MaterialTheme.typography.bodySmall, color = if (r.book.copiesAvailable > 0) c.ok else c.danger)
            }
        }
        TextAction(
            stringResource(R.string.recommend_dismiss), onDismiss, ink = c.ink2,
            modifier = Modifier.padding(start = Spacing.xs, bottom = Spacing.xs).semantics { contentDescription = dismissLabel },
        )
    }
}

/** The short row beside a detail pane: cover, title, reason; the chosen one has an outline. */
@Composable
private fun RecommendRow(r: Recommendation, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.shelfTap
    val shape = RoundedCornerShape(Radius.card)
    Row(
        modifier.fillMaxWidth().pressable(shape, onClick, role = null, row = true).background(if (selected) c.surface else c.glass.copy(alpha = CARD_FILL))
            .then(if (selected) Modifier.border(BorderStroke(Radius.hairline * 2, c.ink3), shape) else Modifier)
            .padding(Spacing.m),
        horizontalArrangement = Arrangement.spacedBy(Spacing.m + Spacing.xs / 2), verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(r.book.coverUrl, r.book.title, Modifier.size(CoverSize.rowWidth, CoverSize.rowHeight), RoundedCornerShape(Spacing.s))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs / 2)) {
            Text(r.book.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
            Text(reasonText(r.reason), style = MaterialTheme.typography.bodySmall, color = c.warn, fontWeight = FontWeight.Bold, maxLines = 2)
        }
    }
}

/** "Why this suggestion": the chosen book's cover, title and author, the reason in a box, the copies left, and the two actions. */
@Composable
private fun RecommendDetail(r: Recommendation, onOpen: () -> Unit, onDismiss: () -> Unit) {
    val c = MaterialTheme.shelfTap
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Cover(r.book.coverUrl, r.book.title, Modifier.size(CoverSize.detailWidth, CoverSize.detailHeight), RoundedCornerShape(Spacing.m), large = true)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(r.book.title, style = MaterialTheme.typography.titleLarge)
            if (r.book.authors.isNotEmpty()) Text(r.book.authors.joinToString(", "), style = MaterialTheme.typography.bodyMedium, color = c.ink2)
        }
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.stat)).background(c.surface).padding(horizontal = Spacing.l, vertical = Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(reasonText(r.reason), style = MaterialTheme.typography.titleSmall, color = c.warn)
        Text(statusText(r.book), style = MaterialTheme.typography.bodyMedium, color = if (r.book.copiesAvailable > 0) c.ok else c.danger)
    }
    PrimaryButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.recommend_open_book)) }
    SecondaryButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.recommend_dismiss)) }
}

/** How many suggestions the strip on the catalog home shows; the screen has them all. */
internal const val RECOMMEND_STRIP = 8

/**
 * "Suggestions for you" on the catalog home: the covers as in the other strips, the reason in words under each, and a line that says where the
 * suggestions come from and where to turn them off. Drawn only when there is something to suggest.
 */
@Composable
internal fun RecommendStrip(items: List<Recommendation>, onOpen: (String) -> Unit, onAll: () -> Unit) {
    val c = MaterialTheme.shelfTap
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        SectionHeader(title = stringResource(R.string.tab_recommend), actionLabel = stringResource(R.string.see_all), onAction = onAll)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            items(items, key = { it.book.titleId }) { r ->
                Column(Modifier.width(GlassSize.cardWidth), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    BookCard(
                        title = r.book.title, author = r.book.authors.joinToString(", "), status = statusText(r.book), onClick = { onOpen(r.book.titleId) },
                        cover = { Cover(r.book.coverUrl, r.book.title, Modifier.fillMaxSize(), shape = androidx.compose.ui.graphics.RectangleShape, large = true) },
                    )
                    Text(reasonText(r.reason), style = MaterialTheme.typography.labelMedium, color = c.warn, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Text(stringResource(R.string.recommend_strip_note), style = MaterialTheme.typography.labelSmall, color = c.ink3)
    }
}
