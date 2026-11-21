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
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navArgument
import androidx.navigation.compose.rememberNavController
import vn.shelftap.app.R
import vn.shelftap.app.ui.catalogue.CatalogueScreen
import vn.shelftap.app.ui.detail.TitleDetailScreen
import vn.shelftap.app.ui.search.SearchScreen

object Routes {
    const val CATALOGUE = "catalogue"
    const val SEARCH = "search"
    /** Argument name "titleId" must match TitleDetailViewModel.ARG_TITLE_ID. */
    const val DETAIL = "detail/{titleId}"

    fun detail(titleId: String): String = "detail/${Uri.encode(titleId)}"
}

@Composable
fun ShelfTapNavHost() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomBar = currentRoute == Routes.CATALOGUE || currentRoute == Routes.SEARCH

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.CATALOGUE,
                        onClick = { navigateTopLevel(navController, Routes.CATALOGUE) },
                        icon = { Icon(Icons.Filled.List, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_catalogue)) },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SEARCH,
                        onClick = { navigateTopLevel(navController, Routes.SEARCH) },
                        icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_search)) },
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
                CatalogueScreen(onTitleClick = { titleId -> navController.navigate(Routes.detail(titleId)) })
            }
            composable(Routes.SEARCH) {
                SearchScreen(onTitleClick = { titleId -> navController.navigate(Routes.detail(titleId)) })
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("titleId") { type = NavType.StringType }),
            ) {
                TitleDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun navigateTopLevel(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(Routes.CATALOGUE) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
