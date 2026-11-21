package vn.shelftap.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import vn.shelftap.app.data.CatalogueRepository

object Routes {
    const val SYNC = "sync"
    const val CATALOGUE = "catalogue"
    const val SEARCH = "search"
    const val DETAIL = "detail/{titleId}"

    fun detail(titleId: String): String = "detail/${Uri.encode(titleId)}"
}

private data class NavTab(val route: String, val label: String, val icon: ImageVector)

/**
 * Top-level navigation. Starts on the download screen until the catalogue is on the device,
 * then shows the Catalogue and Search tabs; the title detail screen sits on top of them.
 */
@Composable
fun ShelfTapRoot(repository: CatalogueRepository) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    val start = remember { if (repository.isCatalogueReady) Routes.CATALOGUE else Routes.SYNC }
    val tabs = remember {
        listOf(
            NavTab(Routes.CATALOGUE, "Catalogue", Icons.AutoMirrored.Filled.List),
            NavTab(Routes.SEARCH, "Search", Icons.Default.Search),
        )
    }
    val showTabs = route == Routes.CATALOGUE || route == Routes.SEARCH

    Scaffold(
        bottomBar = {
            if (showTabs) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(Routes.CATALOGUE) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.SYNC) {
                SyncScreen(
                    repository = repository,
                    onReady = {
                        navController.navigate(Routes.CATALOGUE) {
                            popUpTo(Routes.SYNC) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.CATALOGUE) {
                CatalogueScreen(
                    repository = repository,
                    onOpen = { id -> navController.navigate(Routes.detail(id)) },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    repository = repository,
                    onOpen = { id -> navController.navigate(Routes.detail(id)) },
                )
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("titleId") { type = NavType.StringType }),
            ) { backStackEntry ->
                DetailScreen(
                    repository = repository,
                    titleId = backStackEntry.arguments?.getString("titleId").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
