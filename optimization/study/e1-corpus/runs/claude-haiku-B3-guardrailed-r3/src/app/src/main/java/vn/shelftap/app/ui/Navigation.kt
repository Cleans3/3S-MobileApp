package vn.shelftap.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navArgument
import androidx.navigation.compose.rememberNavController
import vn.shelftap.app.R
import vn.shelftap.app.ShelfTapApp
import vn.shelftap.app.ui.book.BookScreen
import vn.shelftap.app.ui.book.BookViewModel
import vn.shelftap.app.ui.history.HistoryScreen
import vn.shelftap.app.ui.history.HistoryViewModel
import vn.shelftap.app.ui.loans.LoansScreen
import vn.shelftap.app.ui.loans.LoansViewModel

object Routes {
    const val LOANS = "loans"
    const val HISTORY = "history"
    const val BOOK = "book/{titleId}"

    fun book(titleId: String): String = "book/${Uri.encode(titleId)}"
}

private data class TopTab(val route: String, val labelRes: Int, val icon: ImageVector)

private val topTabs = listOf(
    TopTab(Routes.LOANS, R.string.nav_loans, Icons.AutoMirrored.Filled.List),
    TopTab(Routes.HISTORY, R.string.nav_history, Icons.Filled.DateRange),
)

@Composable
fun ShelfTapRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val container = (LocalContext.current.applicationContext as ShelfTapApp).container
    val showTabs = currentRoute == Routes.LOANS || currentRoute == Routes.HISTORY

    Scaffold(
        bottomBar = {
            if (showTabs) {
                NavigationBar {
                    topTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            // The text label names the control, so the icon is decorative.
                            icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOANS,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.LOANS) {
                val viewModel: LoansViewModel = viewModel {
                    LoansViewModel(container.repository, container.defaultDispatcher)
                }
                val state by viewModel.state.collectAsStateWithLifecycle()
                LoansScreen(
                    state = state,
                    onRefresh = viewModel::refresh,
                    onRenew = viewModel::renew,
                    onOpenBook = { titleId -> navController.navigate(Routes.book(titleId)) },
                )
            }

            composable(Routes.HISTORY) {
                val viewModel: HistoryViewModel = viewModel {
                    HistoryViewModel(container.repository, container.defaultDispatcher)
                }
                val state by viewModel.state.collectAsStateWithLifecycle()
                HistoryScreen(
                    state = state,
                    onSwitchChanged = viewModel::onSwitchTapped,
                    onConfirmTurnOff = viewModel::confirmTurnOff,
                    onDismissTurnOff = viewModel::dismissTurnOff,
                    onRefresh = viewModel::load,
                    onOpenBook = { titleId -> navController.navigate(Routes.book(titleId)) },
                )
            }

            composable(
                route = Routes.BOOK,
                arguments = listOf(navArgument("titleId") { type = NavType.StringType }),
            ) { entry ->
                val titleId = entry.arguments?.getString("titleId").orEmpty()
                val viewModel: BookViewModel = viewModel(key = titleId) {
                    BookViewModel(container.repository, titleId)
                }
                val state by viewModel.state.collectAsStateWithLifecycle()
                BookScreen(state = state, onBack = { navController.popBackStack() })
            }
        }
    }
}
