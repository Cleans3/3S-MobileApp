package vn.shelftap.app.ui

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
import androidx.compose.runtime.collectAsState
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
import vn.shelftap.app.data.repository.LibraryRepository
import vn.shelftap.app.ui.detail.BookDetailScreen
import vn.shelftap.app.ui.detail.BookDetailViewModel
import vn.shelftap.app.ui.history.HistoryScreen
import vn.shelftap.app.ui.history.HistoryViewModel
import vn.shelftap.app.ui.loans.LoansScreen
import vn.shelftap.app.ui.loans.LoansViewModel
import vn.shelftap.app.ui.navigation.Screen

@Composable
fun ShelfTapMainApp(
    repository: LibraryRepository,
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
                        icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                        label = { Text(stringResource(R.string.loans_tab)) },
                        selected = currentRoute == Screen.Loans.route,
                        onClick = {
                            navController.navigate(Screen.Loans.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.History, contentDescription = null) },
                        label = { Text(stringResource(R.string.history_tab)) },
                        selected = currentRoute == Screen.History.route,
                        onClick = {
                            navController.navigate(Screen.History.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
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
                val viewModel: LoansViewModel = viewModel(
                    factory = LoansViewModel.provideFactory(repository)
                )
                val uiState by viewModel.uiState.collectAsState()

                LoansScreen(
                    uiState = uiState,
                    onRefresh = { viewModel.loadLoans() },
                    onRenewLoan = { loanId -> viewModel.renewLoan(loanId) },
                    onTitleClick = { titleId ->
                        navController.navigate(Screen.BookDetail.createRoute(titleId))
                    },
                    onClearUserMessage = { viewModel.clearUserMessage() }
                )
            }

            composable(Screen.History.route) {
                val viewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModel.provideFactory(repository)
                )
                val uiState by viewModel.uiState.collectAsState()

                HistoryScreen(
                    uiState = uiState,
                    onRefresh = { viewModel.loadData() },
                    onHistorySwitchToggled = { enabled -> viewModel.onHistorySwitchToggled(enabled) },
                    onConfirmTurnOff = { viewModel.confirmTurnOffHistory() },
                    onDismissDialog = { viewModel.dismissConfirmDialog() },
                    onTitleClick = { titleId ->
                        navController.navigate(Screen.BookDetail.createRoute(titleId))
                    },
                    onClearUserMessage = { viewModel.clearUserMessage() }
                )
            }

            composable(
                route = Screen.BookDetail.route,
                arguments = listOf(navArgument("titleId") { type = NavType.StringType })
            ) { backStackEntry ->
                val titleId = backStackEntry.arguments?.getString("titleId") ?: ""
                val viewModel: BookDetailViewModel = viewModel(
                    factory = BookDetailViewModel.provideFactory(titleId, repository)
                )
                val uiState by viewModel.uiState.collectAsState()

                BookDetailScreen(
                    uiState = uiState,
                    onBackClick = { navController.popBackStack() },
                    onRetry = { viewModel.loadDetail() }
                )
            }
        }
    }
}
