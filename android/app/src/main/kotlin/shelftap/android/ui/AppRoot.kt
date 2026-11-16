package shelftap.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import shelftap.android.session.LogoutReason
import shelftap.android.session.SessionState

/** The session state picks the screen: there is no back stack from a protected screen to the login. */
@Composable
fun AppRoot() {
    val session = LocalContainer.current.session
    val state by session.state.collectAsStateWithLifecycle()
    when (val s = state) {
        SessionState.Loading -> Spinner()
        is SessionState.LoggedOut -> LoggedOutHost(s.reason)
        SessionState.Offline -> OfflineScreen()
        SessionState.Locked -> LockScreen()
        SessionState.PasswordChangeRequired -> ChangePasswordScreen(forced = true, onDone = {})
        SessionState.Active -> MainNav()
    }
}

@Composable
private fun LoggedOutHost(reason: LogoutReason?) {
    var card by rememberSaveable { mutableStateOf(false) }
    if (card) CardLoginScreen(onBack = { card = false }) else LoginScreen(reason, onCard = { card = true })
}

private data class Tab(val route: String, val label: String)

private val tabs = listOf(
    Tab("catalog", "Catalog"), Tab("loans", "Loans"), Tab("checkout", "Checkout"),
    Tab("reservations", "Reserved"), Tab("account", "Account"),
)

@Composable
private fun MainNav() {
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    Scaffold(
        bottomBar = {
            if (tabs.any { it.route == route }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {},
                            label = { Text(tab.label) },
                            alwaysShowLabel = true,
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(nav, startDestination = "catalog") {
                composable("catalog") { CatalogScreen(onOpen = { nav.navigate("title/$it") }) }
                composable("title/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                    TitleDetailScreen(it.arguments?.getString("id").orEmpty())
                }
                composable("loans") { LoansScreen() }
                composable("checkout") { CheckoutScreen() }
                composable("reservations") { ReservationsScreen() }
                composable("account") { AccountScreen(onHistory = { nav.navigate("history") }, onChangePassword = { nav.navigate("password") }) }
                composable("history") { HistoryScreen() }
                composable("password") { ChangePasswordScreen(forced = false, onDone = { nav.popBackStack() }) }
            }
        }
    }
}
