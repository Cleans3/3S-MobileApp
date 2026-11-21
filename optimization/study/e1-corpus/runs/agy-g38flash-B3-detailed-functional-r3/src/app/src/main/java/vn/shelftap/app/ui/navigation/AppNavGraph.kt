package vn.shelftap.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import vn.shelftap.app.R
import vn.shelftap.app.di.AppContainer
import vn.shelftap.app.ui.detail.BookDetailScreen
import vn.shelftap.app.ui.detail.BookDetailViewModel
import vn.shelftap.app.ui.history.HistoryScreen
import vn.shelftap.app.ui.history.HistoryViewModel
import vn.shelftap.app.ui.loans.LoansScreen
import vn.shelftap.app.ui.loans.LoansViewModel

@Composable
fun ShelfTapApp(
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isTopLevelDestination = currentRoute in listOf(Screen.Loans.route, Screen.History.route)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (isTopLevelDestination) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Loans.route,
                        onClick = {
                            if (currentRoute != Screen.Loans.route) {
                                navController.navigate(Screen.Loans.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = stringResource(R.string.nav_loans)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_loans)) }
                    )

                    NavigationBarItem(
                        selected = currentRoute == Screen.History.route,
                        onClick = {
                            if (currentRoute != Screen.History.route) {
                                navController.navigate(Screen.History.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = stringResource(R.string.nav_history)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_history)) }
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
                val loansViewModel: LoansViewModel = viewModel(
                    factory = LoansViewModel.provideFactory(container.loansRepository)
                )
                LoansScreen(
                    viewModel = loansViewModel,
                    onNavigateToDetail = { titleId ->
                        navController.navigate(Screen.BookDetail.createRoute(titleId))
                    }
                )
            }

            composable(Screen.History.route) {
                val historyViewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModel.provideFactory(
                        container.historyRepository,
                        container.memberRepository
                    )
                )
                HistoryScreen(
                    viewModel = historyViewModel,
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
                val titleId = backStackEntry.arguments?.getString("titleId").orEmpty()
                val detailViewModel: BookDetailViewModel = viewModel(
                    factory = BookDetailViewModel.provideFactory(
                        titleId,
                        container.titleRepository
                    )
                )
                BookDetailScreen(
                    viewModel = detailViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
