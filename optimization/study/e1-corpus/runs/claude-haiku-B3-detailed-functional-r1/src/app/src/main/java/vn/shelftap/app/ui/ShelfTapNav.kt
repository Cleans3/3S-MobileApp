package vn.shelftap.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import vn.shelftap.app.ui.detail.BookDetailScreen
import vn.shelftap.app.ui.history.HistoryScreen
import vn.shelftap.app.ui.loans.LoansScreen

private object Routes {
    const val LOANS = "loans"
    const val HISTORY = "history"
    const val DETAIL = "book/{titleId}"
    fun detail(titleId: String) = "book/${Uri.encode(titleId)}"
}

@Composable
fun ShelfTapNav() {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val showTabs = route == Routes.LOANS || route == Routes.HISTORY

    fun openTab(target: String) {
        nav.navigate(target) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showTabs) {
                NavigationBar {
                    NavigationBarItem(
                        selected = route == Routes.LOANS,
                        onClick = { openTab(Routes.LOANS) },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        label = { Text("Loans") },
                    )
                    NavigationBarItem(
                        selected = route == Routes.HISTORY,
                        onClick = { openTab(Routes.HISTORY) },
                        icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                        label = { Text("History") },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = Routes.LOANS,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.LOANS) {
                LoansScreen(onOpenTitle = { nav.navigate(Routes.detail(it)) })
            }
            composable(Routes.HISTORY) {
                HistoryScreen(onOpenTitle = { nav.navigate(Routes.detail(it)) })
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("titleId") { type = NavType.StringType }),
            ) { entry ->
                BookDetailScreen(
                    titleId = entry.arguments?.getString("titleId").orEmpty(),
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}
