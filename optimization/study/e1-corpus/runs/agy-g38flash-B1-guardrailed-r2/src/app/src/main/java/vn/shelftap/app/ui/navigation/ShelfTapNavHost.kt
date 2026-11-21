package vn.shelftap.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import vn.shelftap.app.util.DispatcherProvider

sealed class Screen(val route: String) {
    data object Catalogue : Screen("catalogue")
    data object TitleDetail : Screen("titles/{titleId}") {
        fun createRoute(titleId: String): String = "titles/$titleId"
    }
}

@Composable
fun ShelfTapNavHost(
    repository: CatalogueRepository,
    dispatchers: DispatcherProvider,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Catalogue.route,
        modifier = modifier
    ) {
        composable(Screen.Catalogue.route) {
            val viewModel: CatalogueViewModel = viewModel(
                factory = CatalogueViewModel.Factory(repository, dispatchers)
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
                navArgument("titleId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val titleId = backStackEntry.arguments?.getString("titleId").orEmpty()
            val viewModel: TitleDetailViewModel = viewModel(
                key = titleId,
                factory = TitleDetailViewModel.Factory(titleId, repository, dispatchers)
            )
            TitleDetailScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
