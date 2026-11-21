package vn.shelftap.app.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navArgument
import androidx.navigation.compose.rememberNavController
import androidx.navigation.findStartDestination
import vn.shelftap.app.ui.book.BookScreen
import vn.shelftap.app.ui.history.HistoryScreen
import vn.shelftap.app.ui.loans.LoansScreen

private object Routes {
    const val LOANS = "loans"
    const val HISTORY = "history"
    const val BOOK = "book/{titleId}"
    fun book(titleId: String) = "book/${Uri.encode(titleId)}"
}

@Composable
fun ShelfTapScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = backStackEntry?.destination?.route
    val showBottomBar = current == Routes.LOANS || current == Routes.HISTORY

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = current == Routes.LOANS,
                        onClick = { navController.navigateTopLevel(Routes.LOANS) },
                        icon = { Icon(Icons.Filled.List, contentDescription = null) },
                        label = { Text("Loans") },
                    )
                    NavigationBarItem(
                        selected = current == Routes.HISTORY,
                        onClick = { navController.navigateTopLevel(Routes.HISTORY) },
                        icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                        label = { Text("History") },
                    )
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
                LoansScreen(onOpenBook = { navController.navigate(Routes.book(it)) })
            }
            composable(Routes.HISTORY) {
                HistoryScreen(onOpenBook = { navController.navigate(Routes.book(it)) })
            }
            composable(
                route = Routes.BOOK,
                arguments = listOf(navArgument("titleId") { type = NavType.StringType }),
            ) { entry ->
                BookScreen(
                    titleId = entry.arguments?.getString("titleId").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
