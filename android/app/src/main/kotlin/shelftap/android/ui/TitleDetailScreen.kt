package shelftap.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import shelftap.android.R
import shelftap.android.api.TitleDetail
import shelftap.android.ui.icons.LucideIcon
import shelftap.android.ui.icons.LucideIcons
import shelftap.android.ui.icons.LucideMorphs
import shelftap.android.ui.icons.MorphIcon
import shelftap.android.ui.theme.GlassSize
import shelftap.android.ui.theme.HeroSize
import shelftap.android.ui.theme.Radius
import shelftap.android.ui.theme.ShelfTapText
import shelftap.android.ui.theme.Spacing
import shelftap.android.ui.theme.busyFill
import shelftap.android.ui.theme.coverColors
import shelftap.android.ui.theme.okFill
import shelftap.android.ui.theme.pressScale
import shelftap.android.ui.theme.shelfTap

/** How far the cover's colour is lightened to make the field behind the jacket. */
private const val HERO_FIELD_TINT = 0.35f
private const val BOOK_TILT = -3f
/** The board uses 0.62 over a blur; with no blur the page text would show through the bar, so it is denser. */
private const val BAR_FILL = 0.92f
private const val CHIP_FILL = 0.8f

/**
 * A book's page, built to the board `Detail`: a colour field with the jacket tilted on it that fades into the page, glass back
 * button, subject chips, title, author, two tiles, and a glass bar at the foot with the reserve button (and, for a member,
 * a round button to borrow by tapping the book). [onLogin] is set for a guest: reserving then asks for a login.
 * [onBack] is null when the page sits in a pane beside the list. [onCheckout] opens the self-checkout (members).
 * Not shown, because the API has no such data: the shelf and the loan period, the description.
 */
@Composable
fun TitleDetailScreen(
    titleId: String, onLogin: (() -> Unit)? = null, onCheckout: (() -> Unit)? = null, onBack: (() -> Unit)? = null, barInDock: Boolean = false,
    onSubject: ((String) -> Unit)? = null,
) {
    val vm = appViewModel(key = "title-$titleId") { TitleViewModel(it.repository, titleId) }
    val state by vm.state.collectAsStateWithLifecycle()
    LoadView(state.title, onRetry = vm::load, loading = { SkeletonList(rows = 1, modifier = Modifier.fillMaxSize()) }) { t ->
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Hero(t)
                Info(t, state.message?.asString(), state.error, partial = state.partial, onSubject = onSubject)
                Spacer(Modifier.height(HeroSize.barClearance))
            }
            if (onBack != null) {
                GlassCircleButton(onClick = onBack, description = stringResource(R.string.back_to_catalog), modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(start = GlassSize.page, top = Spacing.s)) {
                    LucideIcon(LucideIcons.ArrowLeft, tint = MaterialTheme.shelfTap.ink, size = GlassSize.menuIcon)
                }
            }
            val owner = remember { Any() }
            val reserve = reserveActionOf(owner, state.busy, state.reserved, state.message != null, guest = onLogin != null, onReserve = onLogin ?: vm::reserve)
            val dock = LocalReserveBar.current.takeIf { barInDock }
            if (dock != null) {
                // The button is drawn by the dock at the foot of the screen (it does not slide with this page): hand it what to say and do.
                SideEffect { dock.value = reserve }
                DisposableEffect(owner) { onDispose { if (dock.value?.owner === owner) dock.value = null } }
            } else {
                ActionBar(reserve, guest = onLogin != null, onCheckout = onCheckout, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

/** The colour field of the cover's colour, the tilted jacket on it, and a fade into the page at the foot. (The board's two soft circles are left out: only the book and its field.) */
@Composable
private fun Hero(t: TitleDetail) {
    val (cover, ink) = coverColors(t.title)
    val field = lerp(cover, Color.White, HERO_FIELD_TINT)
    val page = MaterialTheme.colorScheme.background
    Box(Modifier.fillMaxWidth().height(HeroSize.height).clearAndDecorative().background(field)) {
        Jacket(t, cover, ink, Modifier.align(Alignment.TopCenter).padding(top = HeroSize.jacketTop))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(HeroSize.fade).background(Brush.verticalGradient(listOf(Color.Transparent, page))))
    }
}

/** The tilted jacket: the image when the book has one, else its colour with the title and author in large letters. Fixed-size lettering, as on a real jacket. */
@Composable
private fun Jacket(t: TitleDetail, cover: Color, ink: Color, modifier: Modifier) {
    val shape = RoundedCornerShape(topStart = Radius.spine, topEnd = Radius.cover, bottomEnd = Radius.cover, bottomStart = Radius.spine)
    val url = LocalContainer.current.baseUrl.let { base -> t.coverUrl?.let { base.resolve(it)?.toString() } }
    Box(
        modifier.size(HeroSize.jacketWidth, HeroSize.jacketHeight).graphicsLayer { rotationZ = BOOK_TILT }
            .shadow(HeroSize.jacketShadow, shape).clip(shape).background(cover),
    ) {
        if (url != null) {
            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            FixedScale {
                Column(Modifier.fillMaxSize().padding(start = Spacing.xl + Spacing.xs, top = Spacing.xl, end = Spacing.l + Spacing.xs, bottom = Spacing.l + Spacing.xs), verticalArrangement = Arrangement.SpaceBetween) {
                    Text(t.title, style = ShelfTapText.jacketTitle, color = ink, maxLines = 5)
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs + Spacing.xs / 2)) {
                        Box(Modifier.width(HeroSize.rule).height(HeroSize.ruleThickness).background(ink))
                        Text(t.authors.joinToString(", "), style = MaterialTheme.typography.titleSmall, color = ink, maxLines = 2)
                    }
                }
            }
        }
    }
}

/** Subjects, title, author, availability and ISBN. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Info(t: TitleDetail, message: String?, error: UiText?, partial: Boolean, onSubject: ((String) -> Unit)?) {
    val c = MaterialTheme.shelfTap
    Column(
        Modifier.fillMaxWidth().padding(horizontal = HeroSize.contentInset).offset(y = -HeroSize.overlap),
        verticalArrangement = Arrangement.spacedBy(Spacing.m + Spacing.xs / 2),
    ) {
        if (t.subjects.isNotEmpty()) {
            // A subject opens the search for it. Its touch area is 48 dp tall; the pill itself keeps its size.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                t.subjects.forEach { subject ->
                    val description = stringResource(R.string.title_subject_search, subject)
                    val tap = if (onSubject != null) {
                        Modifier.minimumInteractiveComponentSize().pressable(CircleShape, onClick = { onSubject(subject) }).semantics { contentDescription = description }
                    } else {
                        Modifier.padding(vertical = Spacing.xs).clip(CircleShape)
                    }
                    Text(
                        subject, style = MaterialTheme.typography.labelMedium, color = c.ink2,
                        modifier = tap.background(c.glass.copy(alpha = CHIP_FILL)).padding(horizontal = Spacing.m, vertical = Spacing.xs + Spacing.xs / 2),
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(t.title, style = ShelfTapText.detailTitle)
            if (t.authors.isNotEmpty()) Text(t.authors.joinToString(", "), style = MaterialTheme.typography.bodyLarge, color = c.ink2)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            InfoTile(
                stringResource(R.string.title_status), if (partial) stringResource(R.string.catalog_available, t.copiesAvailable) else stringResource(R.string.title_copies, t.copiesAvailable, t.copiesTotal),
                valueColor = if (t.copiesAvailable > 0) c.ok else c.ink, modifier = Modifier.weight(1f),
            )
            InfoTile("ISBN", t.isbn, valueColor = c.ink, modifier = Modifier.weight(1f))
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = c.ok) }
        ErrorText(error)
    }
}

@Composable
private fun InfoTile(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    val c = MaterialTheme.shelfTap
    Column(modifier.clip(RoundedCornerShape(Radius.tile + Spacing.xs)).background(c.surface).padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs / 2)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.ink3)
        Text(value, style = MaterialTheme.typography.titleSmall, color = valueColor)
    }
}

/** What the reserve button says and does for the page's current state: sending, then done only after the server confirmed; its colour changes with the state. */
@Composable
private fun reserveActionOf(owner: Any, busy: Boolean, reserved: Boolean, confirmed: Boolean, guest: Boolean, onReserve: () -> Unit): ReserveAction {
    val c = MaterialTheme.shelfTap
    val (fill, ink) = when {
        guest -> c.primary to c.onPrimary
        busy -> busyFill to Color.White
        reserved -> okFill to Color.White
        else -> c.primary to c.onPrimary
    }
    val label = stringResource(
        when {
            guest -> R.string.title_reserve_login
            busy -> R.string.title_reserving
            reserved && confirmed -> R.string.title_reserve_confirmed
            reserved -> R.string.title_reserve_done
            else -> R.string.title_reserve
        },
    )
    return ReserveAction(owner, label, fill, ink, enabled = guest || (!busy && !reserved), reserved = !guest && reserved, onClick = onReserve)
}

/**
 * The glass bar at the foot of the page, for a guest and for a window with no dock: a round button to borrow by tapping the book
 * (members) and the reserve button. (A member on a compact window has both in the dock instead, see [ActionDock].)
 */
@Composable
private fun ActionBar(reserve: ReserveAction, guest: Boolean, onCheckout: (() -> Unit)?, modifier: Modifier) {
    val c = MaterialTheme.shelfTap
    GlassSurface(modifier.fillMaxWidth().padding(horizontal = Spacing.l, vertical = Spacing.xl), shape = CircleShape, fill = BAR_FILL) {
        Row(Modifier.padding(Spacing.s), horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
            if (onCheckout != null && !guest) {
                val description = stringResource(R.string.title_borrow_by_tap)
                Box(
                    Modifier.size(HeroSize.barButton).clip(CircleShape).background(c.surface).semantics { contentDescription = description }
                        .clickable(role = Role.Button, onClick = onCheckout),
                    contentAlignment = Alignment.Center,
                ) { LucideIcon(LucideIcons.MockupNfc, tint = c.ink, size = GlassSize.menuIcon) }
            }
            ReserveButton(reserve, Modifier.weight(1f).height(HeroSize.barButton), guest = guest)
        }
    }
}
