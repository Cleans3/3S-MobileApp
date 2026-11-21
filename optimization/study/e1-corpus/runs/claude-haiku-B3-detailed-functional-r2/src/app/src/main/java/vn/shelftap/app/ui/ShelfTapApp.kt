package vn.shelftap.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.findStartDestination
import androidx.navigation.navArgument
import vn.shelftap.app.data.LibraryRepository

private const val LOANS = "loans"
private const val HISTORY = "history"
private const val TITLE = "title/{titleId}"

private fun titleRoute(titleId: String) = "title/${Uri.encode(titleId)}"

private data class TopLevelTab(val route: String, val label: String, val icon: ImageVector)

private val topLevelTabs = listOf(
    TopLevelTab(LOANS, "Loans", Icons.AutoMirrored.Filled.List),
    TopLevelTab(HISTORY, "History", Icons.Filled.DateRange),
)

@Composable
fun ShelfTapApp(library: LibraryRepository) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute == LOANS || currentRoute == HISTORY

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelTabs.forEach { tab ->
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
                            icon = { androidx.compose.material3.Icon(tab.icon, contentDescription = tab.label) },
                            label = { androidx.compose.material3.Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = LOANS,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(LOANS) {
                LoansScreen(
                    library = library,
                    onOpenTitle = { navController.navigate(titleRoute(it)) },
                )
            }
            composable(HISTORY) {
                HistoryScreen(
                    library = library,
                    onOpenTitle = { navController.navigate(titleRoute(it)) },
                )
            }
            composable(
                route = TITLE,
                arguments = listOf(navArgument("titleId") { type = NavType.StringType }),
            ) { entry ->
                TitleDetailScreen(
                    library = library,
                    titleId = entry.arguments?.getString("titleId").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
