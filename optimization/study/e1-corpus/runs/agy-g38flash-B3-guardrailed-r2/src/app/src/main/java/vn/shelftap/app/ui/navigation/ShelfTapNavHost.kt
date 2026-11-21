package vn.shelftap.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import vn.shelftap.app.R
import vn.shelftap.app.ShelfTapApp
import vn.shelftap.app.ui.detail.TitleDetailScreen
import vn.shelftap.app.ui.detail.TitleDetailViewModel
import vn.shelftap.app.ui.history.HistoryScreen
import vn.shelftap.app.ui.history.HistoryViewModel
import vn.shelftap.app.ui.loans.LoansScreen
import vn.shelftap.app.ui.loans.LoansViewModel

@Composable
fun ShelfTapNavHost(
    navController: NavHostController,
    appContainer: ShelfTapApp.AppContainer,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(Screen.Loans.route, Screen.History.route)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                                contentDescription = null // Text label is present
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
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null // Text label is present
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
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Loans.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Loans.route) {
                val viewModel: LoansViewModel = viewModel(
                    factory = LoansViewModel.provideFactory(appContainer.loanRepository)
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                LoansScreen(
                    uiState = uiState,
                    onRenewLoan = viewModel::renewLoan,
                    onLoanClick = { titleId ->
                        navController.navigate(Screen.BookDetail.createRoute(titleId))
                    },
                    onDismissMessage = viewModel::dismissUserMessage,
                    onRetry = viewModel::loadLoans
                )
            }

            composable(Screen.History.route) {
                val viewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModel.provideFactory(
                        historyRepository = appContainer.historyRepository,
                        userRepository = appContainer.userRepository
                    )
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                HistoryScreen(
                    uiState = uiState,
                    onHistorySwitchToggled = viewModel::onHistorySwitchToggled,
                    onConfirmDisableHistory = viewModel::onConfirmDisableHistory,
                    onDismissDisableDialog = viewModel::onDismissDisableDialog,
                    onHistoryItemClick = { titleId ->
                        navController.navigate(Screen.BookDetail.createRoute(titleId))
                    },
                    onRetry = viewModel::loadData
                )
            }

            composable(
                route = Screen.BookDetail.route,
                arguments = listOf(
                    navArgument("titleId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val titleId = backStackEntry.arguments?.getString("titleId") ?: ""
                val viewModel: TitleDetailViewModel = viewModel(
                    factory = TitleDetailViewModel.provideFactory(
                        titleId = titleId,
                        titleRepository = appContainer.titleRepository
                    )
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                TitleDetailScreen(
                    uiState = uiState,
                    onNavigateBack = { navController.popBackStack() },
                    onRetry = viewModel::loadTitleDetails
                )
            }
        }
    }
}
