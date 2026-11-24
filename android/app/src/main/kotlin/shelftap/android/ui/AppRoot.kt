package shelftap.android.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.foundation.shape.CircleShape
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
import shelftap.android.ui.theme.pressScale
import shelftap.android.ui.theme.Spacing

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
private const val PILL_CIRCLE = 0.16f

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

private fun screenEnter(from: String?, to: String?, tier: EffectTier, slidePx: Int, back: Boolean): EnterTransition = when {
    tier == EffectTier.LIGHT -> if (isDeep(from) || isDeep(to)) fadeIn(tween(Motion.SHORT_FADE_MS)) else EnterTransition.None
    isDeep(from) || isDeep(to) ->
        slideInHorizontally(Motion.spec(tier, Motion.SCREEN_MS)) { if (back) -slidePx else slidePx } + fadeIn(Motion.spec(tier, Motion.SCREEN_MS))
    else -> fadeIn(Motion.spec(tier, Motion.FADE_IN_MS, delayMs = Motion.FADE_OUT_MS)) // old screen out first, then the new one in
}

private fun screenExit(from: String?, to: String?, tier: EffectTier, slidePx: Int, back: Boolean): ExitTransition = when {
    tier == EffectTier.LIGHT -> if (isDeep(from) || isDeep(to)) fadeOut(tween(Motion.SHORT_FADE_MS)) else ExitTransition.None
    isDeep(from) || isDeep(to) ->
        slideOutHorizontally(Motion.spec(tier, Motion.SCREEN_MS)) { if (back) slidePx else -slidePx } + fadeOut(Motion.spec(tier, Motion.SCREEN_MS))
    else -> fadeOut(Motion.spec(tier, Motion.FADE_OUT_MS))
}

private fun NavHostController.goTo(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun MainNav(guest: Boolean, reason: LogoutReason?) {
    val nav = rememberNavController()
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
    val slidePx = with(LocalDensity.current) { Motion.slideDistance.roundToPx() }
    val overlayOpen = menuOpen && compact // a window that grew past the compact size has no ☰ menu to keep open
    // Self-checkout is not in the menu: a button that is always in reach on the main screens (members only).
    val pillHere = !guest && title != null && route != CHECKOUT
    val showPill = pillHere && !overlayOpen && compact // the room for it stays while the menu is open, so the page does not jump
    val contentMax = when (layout) {
        NavLayout.COMPACT -> Dp.Unspecified
        NavLayout.RAIL -> AdaptiveSize.contentMax
        NavLayout.SIDE -> if (route == CATALOG) Dp.Unspecified else AdaptiveSize.contentMaxWide
    }

    // Records the page only while the menu is open.
    backdrop.enabled = glass && overlayOpen
    Row(Modifier.fillMaxSize()) {
        if (!compact) {
            SideNav(
                items, current = route, width = if (layout == NavLayout.RAIL) AdaptiveSize.railWidth else AdaptiveSize.sideWidth,
                centred = layout == NavLayout.RAIL, showPill = pillHere, onPill = { nav.goTo(CHECKOUT) }, onSelect = { nav.goTo(it) },
            )
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            Scaffold(
                topBar = {
                    if (title != null) {
                        if (compact && route == CATALOG) {
                            // The mockup's catalog bar: the library's name in the middle, a round account button (the login for a guest).
                            AppTopBar(
                                R.string.catalog_bar_title, overlayOpen, onMenu = { menuOpen = !menuOpen }, centred = true,
                                accountDescription = stringResource(if (guest) R.string.login_title else R.string.open_account),
                                onAccount = { nav.goTo(if (guest) LOGIN else "account") },
                            )
                        } else if (compact) {
                            AppTopBar(title, overlayOpen, onMenu = { menuOpen = !menuOpen })
                        } else {
                            AppTitleBar(title)
                        }
                    }
                },
                floatingActionButton = {
                    if (showPill) {
                        val pillSource = remember { MutableInteractionSource() }
                        ExtendedFloatingActionButton(
                            onClick = { nav.goTo(CHECKOUT) },
                            modifier = Modifier.pressScale(pillSource),
                            interactionSource = pillSource,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            // The NFC glyph in a small glass circle, then the sentence (as in the mockup).
                            Box(Modifier.size(GlassSize.pillCircle).clip(CircleShape).background(MaterialTheme.colorScheme.onPrimary.copy(alpha = PILL_CIRCLE)), contentAlignment = Alignment.Center) {
                                LucideIcon(LucideIcons.MockupNfc, tint = MaterialTheme.colorScheme.onPrimary, size = GlassSize.pillGlyph)
                            }
                            Spacer(Modifier.width(Spacing.m))
                            Text(stringResource(R.string.checkout_pill))
                        }
                    }
                },
                floatingActionButtonPosition = FabPosition.Center,
            ) { padding ->
                // With the menu open the page behind it is out of reach: TalkBack reads the menu, not the screen under it.
                Box(
                    Modifier.fillMaxSize()
                        // A book's page paints its colour field up to the top edge, under the status bar; it pads its own buttons.
                        .padding(if (route?.startsWith("title/") == true) PaddingValues(start = padding.calculateStartPadding(LayoutDirection.Ltr), end = padding.calculateEndPadding(LayoutDirection.Ltr), bottom = padding.calculateBottomPadding()) else padding)
                        .padding(bottom = if (pillHere && compact) Spacing.pillClearance else Spacing.none)
                        // The page is blurred lightly (the mockup's 3 px) and records itself for the strong blur right behind the panel.
                        // Both read their flag in the draw phase and cost nothing while the menu is shut.
                        .graphicsLayer { if (glass && overlayOpen) renderEffect = BlurEffect(blurPx, blurPx, TileMode.Clamp) }
                        .then(if (glass) Modifier.recordBackdrop(backdrop) else Modifier)
                        .then(if (overlayOpen) Modifier.clearAndSetSemantics {} else Modifier),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(Modifier.fillMaxHeight().widthIn(max = contentMax).fillMaxWidth()) {
                        NavHost(
                            nav, startDestination = if (guest && reason != null) LOGIN else CATALOG,
                            enterTransition = { screenEnter(initialState.destination.route, targetState.destination.route, tier, slidePx, back = false) },
                            exitTransition = { screenExit(initialState.destination.route, targetState.destination.route, tier, slidePx, back = false) },
                            popEnterTransition = { screenEnter(initialState.destination.route, targetState.destination.route, tier, slidePx, back = true) },
                            popExitTransition = { screenExit(initialState.destination.route, targetState.destination.route, tier, slidePx, back = true) },
                        ) {
                            composable(CATALOG) {
                                val onLogin = if (guest) ({ nav.navigate(LOGIN) }) else null
                                if (layout == NavLayout.SIDE) {
                                    CatalogTwoPane(onLogin, onCheckout = if (guest) null else ({ nav.goTo(CHECKOUT) }))
                                } else {
                                    CatalogScreen(onOpen = { nav.navigate("title/$it") })
                                }
                            }
                            composable("title/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                                TitleDetailScreen(
                                    it.arguments?.getString("id").orEmpty(), onLogin = if (guest) ({ nav.navigate(LOGIN) }) else null,
                                    onCheckout = if (guest) null else ({ nav.goTo(CHECKOUT) }), onBack = { nav.popBackStack() },
                                )
                            }
                            composable(LOGIN) { LoggedOutHost(reason) }
                            composable("loans") { LoansScreen() }
                            composable(CHECKOUT) { CheckoutScreen() }
                            composable("reservations") { ReservationsScreen() }
                            composable("account") { AccountScreen(onHistory = { nav.navigate("history") }, onChangePassword = { nav.navigate("password") }) }
                            composable("history") { HistoryScreen() }
                            composable("password") { ChangePasswordScreen(forced = false, onDone = { nav.popBackStack() }) }
                        }
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
    AnimatedVisibility(
        visible = visible,
        enter = EnterTransition.None,
        exit = if (tier == EffectTier.LIGHT) ExitTransition.None
        else fadeOut(Motion.spec(tier, Motion.MENU_EXIT_MS)) + scaleOut(Motion.spec(tier, Motion.MENU_EXIT_MS), targetScale = 0.92f, transformOrigin = TransformOrigin(0f, 0f)),
    ) {
        MenuOverlay(items, current = current, backdrop = backdrop, onSelect = onSelect, onDismiss = onDismiss)
    }
}

/** A wide window shows the catalog and the chosen book side by side, so picking a book does not leave the list. */
@Composable
private fun CatalogTwoPane(onLogin: (() -> Unit)?, onCheckout: (() -> Unit)?) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxHeight()) { CatalogScreen(onOpen = { selected = it }) }
        VerticalDivider()
        Box(Modifier.weight(1.3f).fillMaxHeight()) {
            val id = selected
            if (id == null) EmptyState(R.string.catalog_pick_hint) else TitleDetailScreen(id, onLogin = onLogin, onCheckout = onCheckout)
        }
    }
}
