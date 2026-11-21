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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.findStartDestination
import androidx.navigation.navArgument
import vn.shelftap.app.AppContainer
import vn.shelftap.app.R

private object Routes {
    const val CATALOGUE = "catalogue"
    const val SEARCH = "search"
    const val DETAIL = "title/{titleId}"

    fun detail(titleId: String): String = "title/${Uri.encode(titleId)}"
}

private data class TabItem(val route: String, val labelRes: Int, val icon: ImageVector)

private val TABS = listOf(
    TabItem(Routes.CATALOGUE, R.string.tab_catalogue, Icons.Filled.List),
    TabItem(Routes.SEARCH, R.string.tab_search, Icons.Filled.Search),
)

@Composable
fun ShelfTapRoot(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val syncStatus by container.sync.status.collectAsStateWithLifecycle()
    val showTabs = currentRoute == Routes.CATALOGUE || currentRoute == Routes.SEARCH

    Scaffold(
        bottomBar = {
            if (showTabs) {
                NavigationBar {
                    TABS.forEach { tab ->
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
                            // The icon sits next to a text label, so it has no description of its own.
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
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
                val viewModel = viewModel<CatalogueViewModel>(
                    factory = SimpleViewModelFactory { CatalogueViewModel(container.repository) },
                )
                CatalogueScreen(
                    viewModel = viewModel,
                    syncStatus = syncStatus,
                    onRetrySync = { container.sync.start() },
                    onTitleClick = { id -> navController.navigate(Routes.detail(id)) },
                )
            }
            composable(Routes.SEARCH) {
                val viewModel = viewModel<SearchViewModel>(
                    factory = SimpleViewModelFactory { SearchViewModel(container.repository) },
                )
                SearchScreen(
                    viewModel = viewModel,
                    syncStatus = syncStatus,
                    onRetrySync = { container.sync.start() },
                    onTitleClick = { id -> navController.navigate(Routes.detail(id)) },
                )
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("titleId") { type = NavType.StringType }),
            ) { entry ->
                val titleId = entry.arguments?.getString("titleId").orEmpty()
                val viewModel = viewModel<DetailViewModel>(
                    key = "detail-$titleId",
                    factory = SimpleViewModelFactory { DetailViewModel(container.repository, titleId) },
                )
                DetailScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
        }
    }
}
