package vn.shelftap.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import vn.shelftap.app.di.ServiceLocator
import vn.shelftap.app.ui.catalogue.CatalogueScreen
import vn.shelftap.app.ui.catalogue.CatalogueViewModel
import vn.shelftap.app.ui.detail.DetailScreen
import vn.shelftap.app.ui.detail.DetailViewModel

sealed class Screen(val route: String) {
    data object Catalogue : Screen("catalogue")
    data object Detail : Screen("detail/{titleId}") {
        fun createRoute(titleId: String) = "detail/$titleId"
    }
}

@Composable
fun ShelfTapNavGraph(
    navController: NavHostController,
    serviceLocator: ServiceLocator,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Catalogue.route,
        modifier = modifier
    ) {
        composable(Screen.Catalogue.route) {
            val viewModel = CatalogueViewModel(serviceLocator.catalogueRepository)
            CatalogueScreen(
                viewModel = viewModel,
                onTitleClick = { titleId ->
                    navController.navigate(Screen.Detail.createRoute(titleId))
                }
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("titleId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val titleId = backStackEntry.arguments?.getString("titleId").orEmpty()
            val viewModel = DetailViewModel(titleId, serviceLocator.catalogueRepository)
            DetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
