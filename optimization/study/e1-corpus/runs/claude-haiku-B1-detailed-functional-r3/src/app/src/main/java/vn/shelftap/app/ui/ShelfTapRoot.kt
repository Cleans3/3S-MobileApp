package vn.shelftap.app.ui

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
import androidx.navigation.navArgument

object Routes {
    const val CATALOGUE = "catalogue"
    const val SEARCH = "search"
    const val DETAIL = "detail/{titleId}"

    fun detail(titleId: String): String = "detail/${Uri.encode(titleId)}"
}

@Composable
fun ShelfTapRoot() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showTabs = currentRoute == Routes.CATALOGUE || currentRoute == Routes.SEARCH

    Scaffold(
        bottomBar = {
            if (showTabs) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.CATALOGUE,
                        onClick = { navigateTab(navController, Routes.CATALOGUE) },
                        icon = { Icon(Icons.Filled.List, contentDescription = null) },
                        label = { Text("Catalogue") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SEARCH,
                        onClick = { navigateTab(navController, Routes.SEARCH) },
                        icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        label = { Text("Search") },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.CATALOGUE,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.CATALOGUE) {
                CatalogueScreen(onOpen = { id -> navController.navigate(Routes.detail(id)) })
            }
            composable(Routes.SEARCH) {
                SearchScreen(onOpen = { id -> navController.navigate(Routes.detail(id)) })
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("titleId") { type = NavType.StringType }),
            ) { entry ->
                DetailScreen(
                    id = entry.arguments?.getString("titleId").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun navigateTab(controller: NavHostController, route: String) {
    controller.navigate(route) {
        popUpTo(controller.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
