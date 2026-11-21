package vn.shelftap.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import vn.shelftap.app.data.repository.ShelfTapRepository
import vn.shelftap.app.ui.bookdetail.BookDetailScreen
import vn.shelftap.app.ui.bookdetail.BookDetailViewModel
import vn.shelftap.app.ui.history.HistoryScreen
import vn.shelftap.app.ui.history.HistoryViewModel
import vn.shelftap.app.ui.loans.LoansScreen
import vn.shelftap.app.ui.loans.LoansViewModel

data class BottomNavItem(
    val titleRes: Int,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun AppNavigation(
    repository: ShelfTapRepository,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(
            titleRes = R.string.nav_loans,
            route = Screen.Loans.route,
            selectedIcon = Icons.Filled.Book,
            unselectedIcon = Icons.Outlined.Book
        ),
        BottomNavItem(
            titleRes = R.string.nav_history,
            route = Screen.History.route,
            selectedIcon = Icons.Filled.History,
            unselectedIcon = Icons.Outlined.History
        )
    )

    val showBottomBar = currentRoute in listOf(Screen.Loans.route, Screen.History.route)

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = stringResource(item.titleRes)
                                )
                            },
                            label = { Text(stringResource(item.titleRes)) }
                        )
                    }
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
                    factory = LoansViewModel.Factory(repository)
                )
                LoansScreen(
                    viewModel = loansViewModel,
                    onTitleClick = { titleId ->
                        navController.navigate(Screen.BookDetail.createRoute(titleId))
                    }
                )
            }

            composable(Screen.History.route) {
                val historyViewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModel.Factory(repository)
                )
                HistoryScreen(
                    viewModel = historyViewModel,
                    onTitleClick = { titleId ->
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
                val bookDetailViewModel: BookDetailViewModel = viewModel(
                    factory = BookDetailViewModel.Factory(titleId, repository)
                )
                BookDetailScreen(
                    viewModel = bookDetailViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
