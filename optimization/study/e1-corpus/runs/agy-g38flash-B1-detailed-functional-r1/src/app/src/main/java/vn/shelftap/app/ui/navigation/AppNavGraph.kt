package vn.shelftap.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import vn.shelftap.app.data.repository.CatalogueRepository
import vn.shelftap.app.ui.catalogue.CatalogueScreen
import vn.shelftap.app.ui.catalogue.CatalogueViewModel
import vn.shelftap.app.ui.detail.TitleDetailScreen
import vn.shelftap.app.ui.detail.TitleDetailViewModel

/**
 * Navigation graph coordinating Catalogue/Search screen and Title Detail screen.
 */
@Composable
fun AppNavGraph(
    repository: CatalogueRepository,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Catalogue.route
    ) {
        composable(route = Screen.Catalogue.route) {
            val catalogueViewModel: CatalogueViewModel = viewModel(
                factory = CatalogueViewModel.Factory(repository)
            )

            CatalogueScreen(
                viewModel = catalogueViewModel,
                onTitleClick = { titleId ->
                    navController.navigate(Screen.TitleDetail.createRoute(titleId))
                }
            )
        }

        composable(
            route = Screen.TitleDetail.route,
            arguments = listOf(
                navArgument(Screen.TitleDetail.ARG_TITLE_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val titleId = backStackEntry.arguments?.getString(Screen.TitleDetail.ARG_TITLE_ID).orEmpty()
            val detailViewModel: TitleDetailViewModel = viewModel(
                factory = TitleDetailViewModel.Factory(titleId, repository)
            )

            TitleDetailScreen(
                viewModel = detailViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
