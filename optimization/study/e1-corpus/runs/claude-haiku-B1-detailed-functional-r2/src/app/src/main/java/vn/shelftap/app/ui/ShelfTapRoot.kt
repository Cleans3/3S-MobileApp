package vn.shelftap.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import vn.shelftap.app.R
import vn.shelftap.app.data.CatalogueRepository

object Routes {
    const val CATALOGUE = "catalogue"
    const val SEARCH = "search"
    const val DETAIL = "detail/{titleId}"

    fun detail(titleId: String): String = "detail/${Uri.encode(titleId)}"
}

@Composable
fun ShelfTapRoot(repository: CatalogueRepository) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showTabs = route == Routes.CATALOGUE || route == Routes.SEARCH

    fun openTab(target: String) {
        nav.navigate(target) {
            popUpTo(nav.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showTabs) {
                NavigationBar {
                    NavigationBarItem(
                        selected = route == Routes.CATALOGUE,
                        onClick = { openTab(Routes.CATALOGUE) },
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_catalogue)) },
                    )
                    NavigationBarItem(
                        selected = route == Routes.SEARCH,
                        onClick = { openTab(Routes.SEARCH) },
                        icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_search)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.CATALOGUE,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.CATALOGUE) {
                CatalogueScreen(
                    repository = repository,
                    onOpenTitle = { id -> nav.navigate(Routes.detail(id)) },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    repository = repository,
                    onOpenTitle = { id -> nav.navigate(Routes.detail(id)) },
                )
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("titleId") { type = NavType.StringType }),
            ) { backStackEntry ->
                DetailScreen(
                    repository = repository,
                    titleId = backStackEntry.arguments?.getString("titleId").orEmpty(),
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}
