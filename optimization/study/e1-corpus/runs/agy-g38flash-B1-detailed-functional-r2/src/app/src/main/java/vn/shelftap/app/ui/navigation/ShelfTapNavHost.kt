package vn.shelftap.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import vn.shelftap.app.ShelfTapApplication
import vn.shelftap.app.ui.catalogue.CatalogueScreen
import vn.shelftap.app.ui.catalogue.CatalogueViewModel
import vn.shelftap.app.ui.detail.TitleDetailScreen
import vn.shelftap.app.ui.detail.TitleDetailViewModel

@Composable
fun ShelfTapNavHost(
    navController: NavHostController,
    app: ShelfTapApplication
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Catalogue.route
    ) {
        composable(Screen.Catalogue.route) {
            val viewModel: CatalogueViewModel = viewModel(
                factory = CatalogueViewModel.provideFactory(app.repository)
            )
            CatalogueScreen(
                viewModel = viewModel,
                onTitleClick = { titleId ->
                    navController.navigate(Screen.TitleDetail.createRoute(titleId))
                }
            )
        }

        composable(
            route = Screen.TitleDetail.route,
            arguments = listOf(
                navArgument("titleId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val titleId = backStackEntry.arguments?.getString("titleId").orEmpty()
            val viewModel: TitleDetailViewModel = viewModel(
                factory = TitleDetailViewModel.provideFactory(titleId, app.repository)
            )
            TitleDetailScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
