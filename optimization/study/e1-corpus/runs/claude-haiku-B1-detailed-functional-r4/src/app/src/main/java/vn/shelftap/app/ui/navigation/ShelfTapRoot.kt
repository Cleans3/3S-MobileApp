package vn.shelftap.app.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
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
import androidx.navigation.compose.rememberNavController
import androidx.navigation.findStartDestination
import androidx.navigation.navArgument
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.ui.catalogue.CatalogueScreen
import vn.shelftap.app.ui.detail.DetailScreen
import vn.shelftap.app.ui.search.SearchScreen

object Routes {
    const val CATALOGUE = "catalogue"
    const val SEARCH = "search"
    const val DETAIL = "detail/{titleId}"

    fun detail(titleId: String): String = "detail/${Uri.encode(titleId)}"
}

@Composable
fun ShelfTapRoot(repository: CatalogueRepository) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomBar = currentRoute == Routes.CATALOGUE || currentRoute == Routes.SEARCH

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.CATALOGUE,
                        onClick = { navigateToTab(navController, Routes.CATALOGUE) },
                        icon = { Icon(Icons.Filled.List, contentDescription = null) },
                        label = { Text("Catalogue") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SEARCH,
                        onClick = { navigateToTab(navController, Routes.SEARCH) },
                        icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        label = { Text("Search") },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.CATALOGUE,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.CATALOGUE) {
                CatalogueScreen(
                    repository = repository,
                    onOpenTitle = { titleId -> navController.navigate(Routes.detail(titleId)) },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    repository = repository,
                    onOpenTitle = { titleId -> navController.navigate(Routes.detail(titleId)) },
                )
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("titleId") { type = NavType.StringType }),
            ) { entry ->
                val titleId = entry.arguments?.getString("titleId") ?: return@composable
                DetailScreen(
                    repository = repository,
                    titleId = titleId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun navigateToTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
