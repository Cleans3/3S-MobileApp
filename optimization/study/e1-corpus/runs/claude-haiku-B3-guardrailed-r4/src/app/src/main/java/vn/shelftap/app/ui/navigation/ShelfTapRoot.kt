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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import vn.shelftap.app.R
import vn.shelftap.app.ui.book.BookScreen
import vn.shelftap.app.ui.history.HistoryScreen
import vn.shelftap.app.ui.loans.LoansScreen

object Routes {
    const val LOANS = "loans"
    const val HISTORY = "history"
    const val BOOK = "book/{titleId}"

    fun book(titleId: String): String = "book/${Uri.encode(titleId)}"
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ShelfTapRoot() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomBar = currentRoute == Routes.LOANS || currentRoute == Routes.HISTORY

    Scaffold(
        // Exposes Compose test tags as resource ids so UI automation (baseline profiles) can find them.
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.LOANS,
                        onClick = { navigateTopLevel(navController, Routes.LOANS) },
                        icon = { Icon(Icons.Filled.List, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_loans)) },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.HISTORY,
                        onClick = { navigateTopLevel(navController, Routes.HISTORY) },
                        icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_history)) },
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
                LoansScreen(onOpenBook = { titleId -> navController.navigate(Routes.book(titleId)) })
            }
            composable(Routes.HISTORY) {
                HistoryScreen(onOpenBook = { titleId -> navController.navigate(Routes.book(titleId)) })
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

private fun navigateTopLevel(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
