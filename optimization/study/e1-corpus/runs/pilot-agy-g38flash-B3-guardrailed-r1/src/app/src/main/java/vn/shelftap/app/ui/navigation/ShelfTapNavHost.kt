package vn.shelftap.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import vn.shelftap.app.R
import vn.shelftap.app.data.repository.ShelfTapRepository
import vn.shelftap.app.ui.detail.BookDetailScreen
import vn.shelftap.app.ui.detail.BookDetailViewModel
import vn.shelftap.app.ui.history.HistoryScreen
import vn.shelftap.app.ui.history.HistoryViewModel
import vn.shelftap.app.ui.loans.LoansScreen
import vn.shelftap.app.ui.loans.LoansViewModel

@Composable
fun ShelfTapNavHost(
    repository: ShelfTapRepository,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(Screen.Loans.route, Screen.History.route)

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        icon = {
                            // Icon inside control that shows text uses contentDescription = null (Rule 20)
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null
                            )
                        },
                        label = { Text(text = stringResource(R.string.nav_loans)) },
                        selected = currentRoute == Screen.Loans.route,
                        onClick = {
                            navController.navigate(Screen.Loans.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                    NavigationBarItem(
                        icon = {
                            // Icon inside control that shows text uses contentDescription = null (Rule 20)
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null
                            )
                        },
                        label = { Text(text = stringResource(R.string.nav_history)) },
                        selected = currentRoute == Screen.History.route,
                        onClick = {
                            navController.navigate(Screen.History.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Loans.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Loans.route) {
                val viewModel = rememberViewModel { LoansViewModel(repository) }
                LoansScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { titleId ->
                        navController.navigate(Screen.BookDetail.createRoute(titleId))
                    }
                )
            }
            composable(Screen.History.route) {
                val viewModel = rememberViewModel { HistoryViewModel(repository) }
                HistoryScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { titleId ->
                        navController.navigate(Screen.BookDetail.createRoute(titleId))
                    }
                )
            }
            composable(
                route = Screen.BookDetail.route,
                arguments = listOf(
                    navArgument("titleId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val titleId = backStackEntry.arguments?.getString("titleId") ?: ""
                val viewModel = rememberViewModel(titleId) { BookDetailViewModel(titleId, repository) }
                BookDetailScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
@Composable
inline fun <reified T : androidx.lifecycle.ViewModel> rememberViewModel(
    key: String? = null,
    crossinline creator: () -> T
): T {
    return androidx.lifecycle.viewmodel.compose.viewModel(
        key = key,
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <VM : androidx.lifecycle.ViewModel> create(modelClass: Class<VM>): VM {
                return creator() as VM
            }
        }
    )
}
