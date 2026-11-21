package vn.shelftap.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import vn.shelftap.app.AppContainer
import vn.shelftap.app.R
import vn.shelftap.app.ui.catalogue.CatalogueScreen
import vn.shelftap.app.ui.catalogue.CatalogueViewModel
import vn.shelftap.app.ui.detail.DetailScreen
import vn.shelftap.app.ui.detail.DetailViewModel
import vn.shelftap.app.ui.search.SearchScreen
import vn.shelftap.app.ui.search.SearchViewModel

private object Routes {
    const val CATALOGUE = "catalogue"
    const val SEARCH = "search"
    const val DETAIL = "detail/{titleId}"
    const val ARG_TITLE_ID = "titleId"

    fun detail(titleId: String) = "detail/${Uri.encode(titleId)}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfTapApp(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute == Routes.CATALOGUE || currentRoute == Routes.SEARCH

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.CATALOGUE,
                        onClick = { navigateTopLevel(navController, Routes.CATALOGUE) },
                        icon = { Icon(Icons.Filled.List, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_catalogue)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SEARCH,
                        onClick = { navigateTopLevel(navController, Routes.SEARCH) },
                        icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_search)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.CATALOGUE,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.CATALOGUE) {
                val viewModel: CatalogueViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { CatalogueViewModel(container.repository) }
                    }
                )
                CatalogueScreen(
                    viewModel = viewModel,
                    onOpenTitle = { titleId -> navController.navigate(Routes.detail(titleId)) }
                )
            }

            composable(Routes.SEARCH) {
                val viewModel: SearchViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { SearchViewModel(container.repository) }
                    }
                )
                SearchScreen(
                    viewModel = viewModel,
                    onOpenTitle = { titleId -> navController.navigate(Routes.detail(titleId)) }
                )
            }

            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument(Routes.ARG_TITLE_ID) { type = NavType.StringType })
            ) { entry ->
                val titleId = entry.arguments?.getString(Routes.ARG_TITLE_ID).orEmpty()
                val viewModel: DetailViewModel = viewModel(
                    key = titleId,
                    factory = viewModelFactory {
                        initializer { DetailViewModel(container.repository, titleId) }
                    }
                )
                DetailScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
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
