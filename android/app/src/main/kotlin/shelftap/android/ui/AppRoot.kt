package shelftap.android.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.animation.core.animateFloatAsState
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import shelftap.android.R
import shelftap.android.ui.icons.LucideIcon
import shelftap.android.ui.icons.LucideIcons
import shelftap.android.ui.theme.GlassSize
import shelftap.android.session.LogoutReason
import shelftap.android.session.SessionState
import shelftap.android.ui.theme.AdaptiveSize
import shelftap.android.ui.theme.EffectTier
import shelftap.android.ui.theme.LocalEffectTier
import shelftap.android.ui.theme.Motion
import shelftap.android.ui.theme.MenuSize
import shelftap.android.ui.theme.Radius
import shelftap.android.ui.theme.pressScale
import shelftap.android.ui.theme.Spacing
import shelftap.android.ui.theme.shelfTap
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush

/**
 * The session state picks the screen: there is no back stack from a protected screen to the login. Logged out is not a
 * dead end: a guest can browse the catalog (plan §6.2); only borrowing and reserving ask for a login.
 */
@Composable
fun AppRoot() {
    val session = LocalContainer.current.session
    val state by session.state.collectAsStateWithLifecycle()
    when (val s = state) {
        SessionState.Loading -> Spinner()
        is SessionState.LoggedOut -> MainNav(guest = true, reason = s.reason)
        SessionState.Offline -> OfflineScreen()
        SessionState.Locked -> LockScreen()
        SessionState.PasswordChangeRequired -> ChangePasswordScreen(forced = true, onDone = {})
        SessionState.Active -> MainNav(guest = false, reason = null)
    }
}

@Composable
private fun LoggedOutHost(reason: LogoutReason?) {
    var card by rememberSaveable { mutableStateOf(false) }
    if (card) CardLoginScreen(onBack = { card = false }) else LoginScreen(reason, onCard = { card = true })
}

/** The glass circle behind the NFC glyph of the floating button. */

private const val CATALOG = "catalog"
private const val CHECKOUT = "checkout"
private const val LOGIN = "login"

private val memberMenu = listOf(
    MenuItem(CATALOG, R.string.tab_catalog, LucideIcons.MockupCatalog), MenuItem("loans", R.string.tab_loans, LucideIcons.MockupLoans),
    MenuItem("reservations", R.string.tab_reservations, LucideIcons.MockupReservations),
    MenuItem("history", R.string.account_history, LucideIcons.MockupHistory), MenuItem("account", R.string.tab_account, LucideIcons.MockupAccount),
)
private val guestMenu = listOf(MenuItem(CATALOG, R.string.tab_catalog, LucideIcons.MockupCatalog), MenuItem(LOGIN, R.string.login_action, LucideIcons.LogIn))

/** What the top bar calls each screen that has one. Detail and password screens have no bar and use the system back. */
@StringRes
private fun titleOf(route: String?): Int? = when (route) {
    CATALOG -> R.string.tab_catalog
    "loans" -> R.string.tab_loans
    CHECKOUT -> R.string.tab_checkout
    "reservations" -> R.string.tab_reservations
    "history" -> R.string.account_history
    "account" -> R.string.tab_account
    LOGIN -> R.string.login_title // not "Đăng nhập": the button on this screen has that name, and two controls named alike confuse TalkBack
    else -> null
}

/** Screens one level down (list to detail, account to password) slide; screens at the same level fade through. */
private fun isDeep(route: String?) = route?.startsWith("title/") == true || route == "password"

/**
 * Going deeper, the new screen slides in over the old one from the right edge (the old one moves a third of the way to the left behind it);
 * going back, the same in reverse: the screen being left slides out to the right and the one under it returns from the left. Whole-screen
 * movement only, no fade and no scaling, so the page is on screen from the first frame of the move. Each screen paints its own
 * background (see [Screen]), so nothing shows through while two are on screen.
 */
private fun screenEnter(from: String?, to: String?, tier: EffectTier, back: Boolean, gesture: Int = NO_GESTURE): EnterTransition = when {
    tier == EffectTier.LIGHT -> if (isDeep(from) || isDeep(to)) fadeIn(tween(Motion.SHORT_FADE_MS)) else EnterTransition.None
    // A back gesture from the right edge moves the page left with the finger, so the screen under it comes in from the right.
    isDeep(from) || isDeep(to) -> slideInHorizontally(gestureSpec(tier, gesture)) { if (!back) it else if (gesture == EDGE_RIGHT) it / PARALLAX else -it / PARALLAX }
    else -> fadeIn(Motion.spec(tier, Motion.FADE_IN_MS, delayMs = Motion.FADE_OUT_MS)) // old screen out first, then the new one in
}

private fun screenExit(from: String?, to: String?, tier: EffectTier, back: Boolean, gesture: Int = NO_GESTURE): ExitTransition = when {
    tier == EffectTier.LIGHT -> if (isDeep(from) || isDeep(to)) fadeOut(tween(Motion.SHORT_FADE_MS)) else ExitTransition.None
    isDeep(from) || isDeep(to) -> slideOutHorizontally(gestureSpec(tier, gesture)) { if (!back) -it / PARALLAX else if (gesture == EDGE_RIGHT) -it else it }
    else -> fadeOut(Motion.spec(tier, Motion.FADE_OUT_MS))
}

/** The edge a back gesture starts from, as the platform numbers it (`BackEventCompat.EDGE_LEFT` = 0, `EDGE_RIGHT` = 1); [NO_GESTURE] for a button or a call. */
private const val EDGE_RIGHT = 1
private const val NO_GESTURE = -1

/** While the finger drags a back gesture the move follows it one to one (no easing); a button or a call uses the usual curve. */
private fun <T> gestureSpec(tier: EffectTier, gesture: Int): FiniteAnimationSpec<T> =
    if (gesture != NO_GESTURE && tier != EffectTier.LIGHT) tween(Motion.SCREEN_MS, easing = LinearEasing) else Motion.spec(tier, Motion.SCREEN_MS)

/** The screen underneath moves this fraction (1/3) of the width while the one above moves all of it. */
private const val PARALLAX = 3

/**
 * A screen of the navigation graph with its own page colour, so that two screens sliding past each other do not show through.
 * With [rounded] the screen's corners are rounded while it moves in or out (a back gesture drags it by them) and square at rest.
 */
@Composable
private fun AnimatedContentScope.Screen(rounded: Boolean = false, content: @Composable () -> Unit) {
    val tier = LocalEffectTier.current
    val corner by transition.animateDp(transitionSpec = { tween(Motion.SCREEN_MS, easing = LinearEasing) }, label = "screen corner") { state ->
        if (rounded && tier != EffectTier.LIGHT && state != EnterExitState.Visible) Radius.sheet else 0.dp
    }
    Box(
        Modifier.fillMaxSize()
            .graphicsLayer {
                val r = corner.toPx()
                shape = RoundedCornerShape(r)
                clip = r > 0f
            }
            .background(MaterialTheme.colorScheme.background),
    ) { content() }
}

private fun NavHostController.goTo(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun MainNav(guest: Boolean, reason: LogoutReason?) {
    val nav = rememberNavController()
    var navFolded by rememberSaveable { mutableStateOf(false) } // the wide menu folded into the rail by the member
    val reserveBar = remember { mutableStateOf<ReserveAction?>(null) } // what the book page's reserve button says, drawn by the dock
    val subjectSearch = LocalContainer.current.subjectSearch
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val layout = rememberNavLayout()
    val compact = layout == NavLayout.COMPACT
    val items = if (guest) guestMenu else memberMenu
    val title = titleOf(route)
    val tier = LocalEffectTier.current
    val glass = tier != EffectTier.LIGHT
    val blurPx = with(LocalDensity.current) { MenuSize.pageBlur.toPx() }
    val backdrop = rememberBackdrop()
    val overlayOpen = menuOpen && compact // a window that grew past the compact size has no ☰ menu to keep open
    // The whole page blurs and clears with the menu, all at once and by one value (the panel has no blur of its own).
    val pageBlur by animateFloatAsState(if (glass && overlayOpen) 1f else 0f, Motion.spec(tier, Motion.MENU_MS), label = "page blur")
    // Self-checkout is not in the menu: a button that is always in reach on the main screens (members only).
    val pillHere = !guest && title != null && route != CHECKOUT
    // A book is open: its page (a route), or on a wide window its pane (the page has published its reserve button).
    val bookOpen = route?.startsWith("title/") == true || (layout == NavLayout.SIDE && reserveBar.value != null)
    val dockOnBar = !guest && bookOpen
    val showDock = !guest && (pillHere || dockOnBar) && !overlayOpen
    val contentMax = when (layout) {
        NavLayout.COMPACT -> Dp.Unspecified
        NavLayout.RAIL -> AdaptiveSize.contentMax
        NavLayout.SIDE -> if (route == CATALOG) Dp.Unspecified else AdaptiveSize.contentMaxWide
    }

    Row(Modifier.fillMaxSize()) {
        if (!compact) {
            SideNav(
                items, current = route, width = if (layout == NavLayout.RAIL || navFolded) AdaptiveSize.railWidth else AdaptiveSize.sideWidth,
                centred = layout == NavLayout.RAIL || navFolded, showPill = false, onPill = { nav.goTo(CHECKOUT) }, onSelect = { nav.goTo(it) },
                onToggle = if (layout == NavLayout.SIDE) ({ navFolded = !navFolded }) else null,
            )
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            Scaffold(
                topBar = {
                    val onBookPage = route?.startsWith("title/") == true
                    if (title != null || (compact && onBookPage)) {
                        if (compact && (route == CATALOG || onBookPage)) {
                            // The mockup's catalog bar: the library's name in the middle, a round account button (the login for a guest).
                            // On a book's page the one bar stays and its menu button turns into the back button (same place, same button).
                            AppTopBar(
                                R.string.catalog_bar_title, overlayOpen, onMenu = { menuOpen = !menuOpen }, centred = true,
                                accountDescription = stringResource(if (guest) R.string.login_title else R.string.open_account),
                                onAccount = { nav.goTo(if (guest) LOGIN else "account") },
                                back = onBookPage, onBack = { nav.popBackStack() },
                            )
                        } else if (title != null) {
                            if (compact) AppTopBar(title, overlayOpen, onMenu = { menuOpen = !menuOpen }) else AppTitleBar(title)
                        }
                    }
                },
                floatingActionButton = {
                    // On a book's page the same button stays, folded to its icon, in the place of the reserve bar's round "borrow" button.
                    if (compact && showDock) ActionDock(onBar = dockOnBar, reserve = reserveBar.value, onCheckout = { nav.goTo(CHECKOUT) })
                },
                floatingActionButtonPosition = FabPosition.Center,
            ) { padding ->
                // With the menu open the page behind it is out of reach: TalkBack reads the menu, not the screen under it.
                // The page is drawn edge to edge: it scrolls under the status bar, the top bar and the floating button, and each screen
                // keeps its first and last item clear of them with [pagePadding]. A book's page paints its colour field up to the top edge itself.
                val titleRoute = route?.startsWith("title/") == true
                val insets = if (titleRoute) PageInsets() else PageInsets(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + (if (pillHere) Spacing.pillClearance else Spacing.none),
                )
                Box(
                    Modifier.fillMaxSize()
                        .padding(
                            start = padding.calculateStartPadding(LayoutDirection.Ltr), end = padding.calculateEndPadding(LayoutDirection.Ltr),
                            bottom = if (titleRoute) padding.calculateBottomPadding() else Spacing.none,
                        )
                        // The page is blurred lightly (the mockup's 3 px) and records itself for the strong blur right behind the panel.
                        // Both read their flag in the draw phase and cost nothing while the menu is shut.
                        .graphicsLayer { if (pageBlur > 0.01f) renderEffect = BlurEffect(blurPx * pageBlur, blurPx * pageBlur, TileMode.Clamp) }
                        .then(if (glass) Modifier.recordBackdrop(backdrop) else Modifier)
                        .then(if (overlayOpen) Modifier.clearAndSetSemantics {} else Modifier),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    CompositionLocalProvider(LocalPageInsets provides insets, LocalReserveBar provides reserveBar) {
                    Box(Modifier.fillMaxHeight().widthIn(max = contentMax).fillMaxWidth()) {
                        NavHost(
                            nav, startDestination = if (guest && reason != null) LOGIN else CATALOG,
                            enterTransition = { screenEnter(initialState.destination.route, targetState.destination.route, tier, back = false) },
                            exitTransition = { screenExit(initialState.destination.route, targetState.destination.route, tier, back = false) },
                            popEnterTransition = { screenEnter(initialState.destination.route, targetState.destination.route, tier, back = true) },
                            popExitTransition = { screenExit(initialState.destination.route, targetState.destination.route, tier, back = true) },
                            // The back gesture: the platform default shrinks the page to 70% and fades it; here it slides like the button's back does.
                            predictivePopEnterTransition = { edge -> screenEnter(initialState.destination.route, targetState.destination.route, tier, back = true, gesture = edge) },
                            predictivePopExitTransition = { edge -> screenExit(initialState.destination.route, targetState.destination.route, tier, back = true, gesture = edge) },
                        ) {
                            composable(CATALOG) {
                                Screen {
                                    val onLogin = if (guest) ({ nav.navigate(LOGIN) }) else null
                                    if (layout == NavLayout.SIDE) {
                                        CatalogTwoPane(onLogin, onCheckout = if (guest) null else ({ nav.goTo(CHECKOUT) }))
                                    } else {
                                        CatalogScreen(onOpen = { nav.navigate("title/$it") })
                                    }
                                }
                            }
                            composable("title/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                                Screen(rounded = true) {
                                    TitleDetailScreen(
                                        it.arguments?.getString("id").orEmpty(), onLogin = if (guest) ({ nav.navigate(LOGIN) }) else null,
                                        onCheckout = if (guest) null else ({ nav.goTo(CHECKOUT) }),
                                        // On a compact window the top bar's own button is the back button; the page draws one only without that bar.
                                        onBack = if (compact) null else ({ nav.popBackStack() }), barInDock = !guest,
                                        // A subject on the page becomes the catalog's search: back to the catalog, which takes it.
                                        onSubject = { subject ->
                                            subjectSearch.value = subject
                                            if (!nav.popBackStack(CATALOG, false)) nav.goTo(CATALOG)
                                        },
                                    )
                                }
                            }
                            composable(LOGIN) { Screen { LoggedOutHost(reason) } }
                            composable("loans") { Screen { LoansScreen() } }
                            composable(CHECKOUT) { Screen { CheckoutScreen(onLoans = { nav.goTo("loans") }) } }
                            composable("reservations") { Screen { ReservationsScreen() } }
                            composable("account") { Screen { AccountScreen(onHistory = { nav.navigate("history") }, onChangePassword = { nav.navigate("password") }) } }
                            composable("history") { Screen { HistoryScreen(onAccount = { nav.goTo("account") }) } }
                            composable("password") { Screen(rounded = true) { ChangePasswordScreen(forced = false, onDone = { nav.popBackStack() }) } }
                        }
                        // On a wide window the dock is a round icon in the page's lower right corner and grows into the reserve bar when a book
                        // is open. It lives in this box, which is as wide as the page, so the bar never runs past the page's edge.
                        if (!compact && showDock) {
                            ActionDock(
                                onBar = dockOnBar, reserve = reserveBar.value, onCheckout = { nav.goTo(CHECKOUT) },
                                modifier = Modifier.align(Alignment.BottomEnd)
                                    .padding(end = Spacing.xl, bottom = Spacing.xl + (if (titleRoute) Spacing.none else padding.calculateBottomPadding())),
                                restIcon = true, lift = false, sideMargin = Spacing.none, maxBarWidth = AdaptiveSize.dockBarMax,
                                maxBarFraction = if (layout == NavLayout.SIDE) SIDE_PANE_SHARE else 1f, // two panes: the bar stays in the book's pane
                            )
                        }
                    }
                    }
                    // Soft fades of the page colour where the page runs under the bars, so a title or the floating button always sits on
                    // something calm. (Where the tier allows, the blur of the mockup goes here too.)
                    if (!titleRoute) {
                        FadeBand(insets.top - Spacing.touch / 2, fromTop = true, modifier = Modifier.align(Alignment.TopCenter)) // status bar and half the top bar
                        // the system inset and the lower half of the floating button and its margin
                        if (pillHere && compact) FadeBand(insets.bottom - Spacing.pillClearance / 2, fromTop = false, modifier = Modifier.align(Alignment.BottomCenter))
                    }
                }
            }
            MenuLayer(
                visible = overlayOpen, tier = tier, items = items, current = route, backdrop = if (glass) backdrop else null,
                onSelect = { menuOpen = false; nav.goTo(it) }, onDismiss = { menuOpen = false },
            )
        }
    }
}

/**
 * The menu panel with its exit: it leaves the way it came, shrinking toward the button's corner and fading (200 ms); at once on
 * the light tier. A separate function, because inside the navigation [Row] a bare AnimatedVisibility would take the row as its receiver.
 */
@Composable
private fun MenuLayer(
    visible: Boolean, tier: EffectTier, items: List<MenuItem>, current: String?, backdrop: BackdropState?, onSelect: (String) -> Unit, onDismiss: () -> Unit,
) {
    // One value runs the panel and the dimming: up to 1 when the menu opens, back to 0 when it closes (at once on the
    // light tier). The overlay stays composed while it runs down, so the exit is the opening played backwards, with nothing scaled.
    val open = remember { Animatable(0f) }
    val panel = remember { Animatable(0f) }
    LaunchedEffect(visible, tier) { open.animateTo(if (visible) 1f else 0f, if (visible) Motion.spec(tier, Motion.MENU_MS) else Motion.exitSpec(tier, Motion.MENU_EXIT_MS)) }
    LaunchedEffect(visible, tier) { panel.animateTo(if (visible) 1f else 0f, if (visible) Motion.spec(tier, Motion.PANEL_IN_MS) else Motion.exitSpec(tier, Motion.PANEL_OUT_MS)) }
    // The page is recorded for the panel's own blur from the moment the menu opens until the panel is gone, not until it starts to close:
    // when the recording stopped at once, the panel went on drawing the page as it was when it opened, a frozen blurred rectangle over a page
    // that was already coming back into focus.
    if (backdrop != null) {
        LaunchedEffect(visible, backdrop) {
            if (visible) {
                backdrop.enabled = true
            } else {
                snapshotFlow { open.value <= 0f && panel.value <= 0f }.first { it }
                backdrop.enabled = false
            }
        }
    }
    if (visible || open.value > 0f || panel.value > 0f) {
        MenuOverlay(items, current = current, backdrop = backdrop, open = { open.value }, panel = { panel.value }, interactive = visible, onSelect = onSelect, onDismiss = onDismiss)
    }
}

/** A band that fades the page colour out over [height]: solid at the screen's edge, clear at the other end. Draws only, never takes a touch. */
@Composable
internal fun FadeBand(height: Dp, fromTop: Boolean, modifier: Modifier = Modifier) {
    val bg = MaterialTheme.shelfTap.bg
    val solid = bg.copy(alpha = FADE_SOLID)
    val stops = if (fromTop) arrayOf(0f to solid, FADE_HOLD to solid, 1f to bg.copy(alpha = 0f)) else arrayOf(0f to bg.copy(alpha = 0f), 1f - FADE_HOLD to solid, 1f to solid)
    Box(modifier.fillMaxWidth().height(height).background(Brush.verticalGradient(colorStops = stops)))
}

/** How opaque the fade is where it meets the edge, and how much of its height stays that way before it fades. */
private const val FADE_SOLID = 0.6f
private const val FADE_HOLD = 0.25f

/** The share of the two-pane window that the book's pane takes (the weights 1 and 1.3 of [CatalogTwoPane]). */
private const val SIDE_PANE_SHARE = 1.3f / 2.3f // the weights 1 and 1.3 (PANE_WEIGHT) of the two panes

/** A wide window shows the catalog and the chosen book side by side, so picking a book does not leave the list. */
@Composable
private fun CatalogTwoPane(onLogin: (() -> Unit)?, onCheckout: (() -> Unit)?) {
    val subjectSearch = LocalContainer.current.subjectSearch
    val tier = LocalEffectTier.current
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    // The back gesture closes the pane first. The book's pane takes its room only while a book is open and gives it back to the list
    // when it is closed (the cross on the page): its share of the width eases between 0 and 1.3.
    BackHandler(enabled = selected != null) { selected = null }
    val share by animateFloatAsState(if (selected != null) PANE_WEIGHT else 0f, Motion.spec(tier, Motion.SCREEN_MS), label = "pane")
    var shown by remember { mutableStateOf(selected) } // the last book, kept on screen while the pane closes
    if (selected != null) shown = selected
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxHeight()) { CatalogScreen(onOpen = { selected = it }) }
        val id = shown
        if (id != null && share > 0.01f) {
            VerticalDivider()
            Box(Modifier.weight(share).fillMaxHeight().clipToBounds()) {
                // The book's colour field runs to the top edge of the pane, as on a phone: no strip is kept free for a bar (the window's title is
                // over the list, not over this pane).
                TitleDetailScreen(
                    id, onLogin = onLogin, onCheckout = onCheckout, onSubject = { subjectSearch.value = it }, barInDock = onLogin == null,
                    onClose = { selected = null },
                )
            }
        }
    }
}

private const val PANE_WEIGHT = 1.3f
