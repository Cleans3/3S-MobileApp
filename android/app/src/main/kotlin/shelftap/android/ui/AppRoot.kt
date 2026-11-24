package shelftap.android.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import shelftap.android.session.LogoutReason
import shelftap.android.session.SessionState

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

private const val CATALOG = "catalog"
private const val CHECKOUT = "checkout"
private const val LOGIN = "login"

private val memberMenu = listOf(
    MenuItem(CATALOG, R.string.tab_catalog), MenuItem("loans", R.string.tab_loans), MenuItem("reservations", R.string.tab_reservations),
    MenuItem("history", R.string.account_history), MenuItem("account", R.string.tab_account),
)
private val guestMenu = listOf(MenuItem(CATALOG, R.string.tab_catalog), MenuItem(LOGIN, R.string.login_action))

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
    val items = if (guest) guestMenu else memberMenu
    val title = titleOf(route)
    // Self-checkout is not in the menu: it is a button that is always in reach on the main screens (members only).
    val showPill = !guest && title != null && route != CHECKOUT && !menuOpen

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { if (title != null) AppTopBar(title, menuOpen, onMenu = { menuOpen = !menuOpen }) },
            floatingActionButton = {
                if (showPill) {
                    ExtendedFloatingActionButton(
                        onClick = { nav.goTo(CHECKOUT) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) { Text(stringResource(R.string.checkout_pill)) }
                }
            },
            floatingActionButtonPosition = FabPosition.Center,
        ) { padding ->
            // With the menu open the page behind it is out of reach: TalkBack reads the menu, not the screen under it.
            Box(Modifier.fillMaxSize().padding(padding).then(if (menuOpen) Modifier.clearAndSetSemantics {} else Modifier)) {
                NavHost(nav, startDestination = if (guest && reason != null) LOGIN else CATALOG) {
                    composable(CATALOG) { CatalogScreen(onOpen = { nav.navigate("title/$it") }) }
                    composable("title/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                        TitleDetailScreen(it.arguments?.getString("id").orEmpty(), onLogin = if (guest) ({ nav.navigate(LOGIN) }) else null)
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
        if (menuOpen) {
            MenuOverlay(
                items, current = route,
                onSelect = { menuOpen = false; nav.goTo(it) },
                onDismiss = { menuOpen = false },
            )
        }
    }
}
