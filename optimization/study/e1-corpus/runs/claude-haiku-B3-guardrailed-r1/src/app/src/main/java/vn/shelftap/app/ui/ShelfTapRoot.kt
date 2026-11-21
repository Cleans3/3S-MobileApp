package vn.shelftap.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.automirrored.filled.List as ListIcon
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navArgument
import androidx.navigation.compose.rememberNavController
import androidx.navigation.findStartDestination
import vn.shelftap.app.R
import vn.shelftap.app.appContainer
import vn.shelftap.app.ui.history.HistoryScreen
import vn.shelftap.app.ui.history.HistoryViewModel
import vn.shelftap.app.ui.loans.LoansScreen
import vn.shelftap.app.ui.loans.LoansViewModel
import vn.shelftap.app.ui.title.TitleScreen
import vn.shelftap.app.ui.title.TitleViewModel

private const val LOANS_ROUTE = "loans"
private const val HISTORY_ROUTE = "history"
private const val TITLE_ARG = "titleId"
private const val TITLE_ROUTE = "title/{$TITLE_ARG}"

private fun titleRoute(titleId: String): String = "title/${Uri.encode(titleId)}"

@Composable
fun ShelfTapRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showTabs = currentRoute == LOANS_ROUTE || currentRoute == HISTORY_ROUTE
    val repository = LocalContext.current.appContainer().libraryRepository

    Scaffold(
        bottomBar = {
            if (showTabs) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == LOANS_ROUTE,
                        onClick = { navController.navigateToTab(LOANS_ROUTE) },
                        icon = { Icon(ListIcon, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_loans)) },
                    )
                    NavigationBarItem(
                        selected = currentRoute == HISTORY_ROUTE,
                        onClick = { navController.navigateToTab(HISTORY_ROUTE) },
                        icon = { Icon(DateRange, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_history)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = LOANS_ROUTE,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(LOANS_ROUTE) {
                val viewModel: LoansViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { LoansViewModel(repository) }
                    },
                )
                LoansScreen(
                    viewModel = viewModel,
                    onOpenTitle = { titleId -> navController.navigate(titleRoute(titleId)) },
                )
            }

            composable(HISTORY_ROUTE) {
                val viewModel: HistoryViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { HistoryViewModel(repository) }
                    },
                )
                HistoryScreen(
                    viewModel = viewModel,
                    onOpenTitle = { titleId -> navController.navigate(titleRoute(titleId)) },
                )
            }

            composable(
                route = TITLE_ROUTE,
                arguments = listOf(navArgument(TITLE_ARG) { type = NavType.StringType }),
            ) { entry ->
                val titleId = entry.arguments?.getString(TITLE_ARG).orEmpty()
                val viewModel: TitleViewModel = viewModel(
                    key = titleId,
                    factory = viewModelFactory {
                        initializer { TitleViewModel(repository, titleId) }
                    },
                )
                TitleScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun NavController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination.id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
