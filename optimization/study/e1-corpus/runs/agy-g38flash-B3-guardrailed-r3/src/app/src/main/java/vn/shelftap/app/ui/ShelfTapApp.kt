package vn.shelftap.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import vn.shelftap.app.R
import vn.shelftap.app.data.repository.AuthRepository
import vn.shelftap.app.data.repository.ShelfTapRepository
import vn.shelftap.app.ui.navigation.Screen
import vn.shelftap.app.ui.screens.detail.BookDetailScreen
import vn.shelftap.app.ui.screens.detail.BookDetailViewModel
import vn.shelftap.app.ui.screens.history.HistoryScreen
import vn.shelftap.app.ui.screens.history.HistoryViewModel
import vn.shelftap.app.ui.screens.loans.LoansScreen
import vn.shelftap.app.ui.screens.loans.LoansViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfTapApp(
    repository: ShelfTapRepository,
    authRepository: AuthRepository,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isTopLevelDestination = currentRoute == Screen.Loans.route || currentRoute == Screen.History.route

    Scaffold(
        topBar = {
            if (isTopLevelDestination) {
                TopAppBar(
                    title = {
                        val titleRes = if (currentRoute == Screen.Loans.route) {
                            R.string.screen_loans_title
                        } else {
                            R.string.screen_history_title
                        }
                        Text(
                            text = stringResource(titleRes),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                )
            }
        },
        bottomBar = {
            if (isTopLevelDestination) {
                NavigationBar {
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
                                imageVector = Icons.Default.Book,
                                contentDescription = stringResource(R.string.cd_nav_loans)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_loans)) },
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
                                imageVector = Icons.Default.History,
                                contentDescription = stringResource(R.string.cd_nav_history)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_history)) },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Loans.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Loans.route) {
                val loansViewModel: LoansViewModel = viewModel(
                    factory = LoansViewModel.provideFactory(repository)
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
                    factory = HistoryViewModel.provideFactory(repository)
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
                arguments = listOf(navArgument("titleId") { type = NavType.StringType })
            ) { backStackEntry ->
                val titleId = backStackEntry.arguments?.getString("titleId").orEmpty()
                val detailViewModel: BookDetailViewModel = viewModel(
                    factory = BookDetailViewModel.provideFactory(repository)
                )
                BookDetailScreen(
                    titleId = titleId,
                    viewModel = detailViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
