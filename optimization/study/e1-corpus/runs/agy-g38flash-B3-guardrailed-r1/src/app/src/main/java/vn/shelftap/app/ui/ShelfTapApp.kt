package vn.shelftap.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
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
import androidx.compose.ui.unit.dp
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
import vn.shelftap.app.ui.bookdetail.BookDetailScreen
import vn.shelftap.app.ui.bookdetail.BookDetailViewModel
import vn.shelftap.app.ui.history.HistoryScreen
import vn.shelftap.app.ui.history.HistoryViewModel
import vn.shelftap.app.ui.loans.LoansScreen
import vn.shelftap.app.ui.loans.LoansViewModel
import vn.shelftap.app.ui.navigation.Screen

@Composable
fun ShelfTapApp(
    repository: LibraryRepository
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(Screen.Loans.route, Screen.History.route)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    // Rule 20: Icon has contentDescription = null because label is present
                    NavigationBarItem(
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
                        icon = {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = null
                            )
                        },
                        label = { Text(text = stringResource(R.string.nav_loans)) },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    )

                    NavigationBarItem(
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
                        icon = {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null
                            )
                        },
                        label = { Text(text = stringResource(R.string.nav_history)) },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
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
                    factory = LoansViewModel.provideFactory(repository)
                )
                val uiState by loansViewModel.uiState.collectAsState()

                LoansScreen(
                    uiState = uiState,
                    onRenewLoan = { loanId -> loansViewModel.renewLoan(loanId) },
                    onSelectBook = { titleId ->
                        navController.navigate(Screen.BookDetail.createRoute(titleId))
                    },
                    onRefresh = { loansViewModel.loadLoans() },
                    onClearMessage = { loansViewModel.clearInfoMessage() }
                )
            }

            composable(Screen.History.route) {
                val historyViewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModel.provideFactory(repository)
                )
                val uiState by historyViewModel.uiState.collectAsState()

                HistoryScreen(
                    uiState = uiState,
                    onSwitchToggled = { enabled -> historyViewModel.onSwitchToggled(enabled) },
                    onConfirmDisable = { historyViewModel.onConfirmDisableHistory() },
                    onDismissDialog = { historyViewModel.onDismissConfirmDialog() },
                    onSelectBook = { titleId ->
                        navController.navigate(Screen.BookDetail.createRoute(titleId))
                    },
                    onRefresh = { historyViewModel.loadHistory() }
                )
            }

            composable(
                route = Screen.BookDetail.route,
                arguments = listOf(navArgument("titleId") { type = NavType.StringType })
            ) { backStackEntry ->
                val titleId = backStackEntry.arguments?.getString("titleId") ?: ""
                val bookDetailViewModel: BookDetailViewModel = viewModel(
                    factory = BookDetailViewModel.provideFactory(titleId, repository)
                )
                val uiState by bookDetailViewModel.uiState.collectAsState()

                BookDetailScreen(
                    uiState = uiState,
                    onBack = { navController.popBackStack() },
                    onRefresh = { bookDetailViewModel.loadBookDetail() }
                )
            }
        }
    }
}
