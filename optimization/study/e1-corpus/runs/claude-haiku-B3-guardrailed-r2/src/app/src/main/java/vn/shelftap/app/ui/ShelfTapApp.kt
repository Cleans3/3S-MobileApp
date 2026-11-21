package vn.shelftap.app.ui

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navArgument
import androidx.navigation.compose.rememberNavController
import androidx.navigation.findStartDestination
import vn.shelftap.app.AppContainer
import vn.shelftap.app.R
import vn.shelftap.app.ui.book.BookDetailScreen
import vn.shelftap.app.ui.history.HistoryScreen
import vn.shelftap.app.ui.loans.LoansScreen

private object Routes {
    const val LOANS = "loans"
    const val HISTORY = "history"
    const val BOOK = "title/{titleId}"

    fun book(titleId: String): String = "title/${Uri.encode(titleId)}"
}

private data class TabItem(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector,
)

private val tabs = listOf(
    TabItem(Routes.LOANS, R.string.tab_loans, Icons.Filled.List),
    TabItem(Routes.HISTORY, R.string.tab_history, Icons.Filled.Refresh),
)

@Composable
fun ShelfTapApp(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showTabs = tabs.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showTabs) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOANS,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.LOANS) {
                LoansScreen(
                    library = container.library,
                    onOpenBook = { titleId -> navController.navigate(Routes.book(titleId)) },
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    library = container.library,
                    onOpenBook = { titleId -> navController.navigate(Routes.book(titleId)) },
                )
            }
            composable(
                route = Routes.BOOK,
                arguments = listOf(navArgument("titleId") { type = NavType.StringType }),
            ) { entry ->
                BookDetailScreen(
                    library = container.library,
                    titleId = entry.arguments?.getString("titleId").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
