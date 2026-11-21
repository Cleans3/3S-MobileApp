package vn.shelftap.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import vn.shelftap.app.data.repository.CatalogueRepository
import vn.shelftap.app.ui.catalogue.CatalogueScreen
import vn.shelftap.app.ui.catalogue.CatalogueViewModel
import vn.shelftap.app.ui.detail.TitleDetailScreen
import vn.shelftap.app.ui.detail.TitleDetailViewModel

@Composable
fun ShelfTapNavHost(
    navController: NavHostController,
    repository: CatalogueRepository,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Catalogue.route,
        modifier = modifier
    ) {
        composable(route = Screen.Catalogue.route) {
            val catalogueViewModel: CatalogueViewModel = viewModel(
                factory = CatalogueViewModel.provideFactory(repository)
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
                navArgument("titleId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val titleId = backStackEntry.arguments?.getString("titleId") ?: ""
            val detailViewModel: TitleDetailViewModel = viewModel(
                factory = TitleDetailViewModel.provideFactory(titleId, repository)
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
