package vn.shelftap.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import vn.shelftap.app.ui.catalogue.CatalogueScreen
import vn.shelftap.app.ui.catalogue.CatalogueViewModel
import vn.shelftap.app.ui.detail.TitleDetailScreen
import vn.shelftap.app.ui.detail.TitleDetailViewModel

object Screen {
    const val Catalogue = "catalogue"
    const val Detail = "detail/{titleId}"

    fun detailRoute(titleId: String): String = "detail/$titleId"
}

@Composable
fun ShelfTapNavHost(
    viewModelFactory: ViewModelProvider.Factory,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Catalogue,
        modifier = modifier
    ) {
        composable(route = Screen.Catalogue) {
            val catalogueViewModel: CatalogueViewModel = viewModel(factory = viewModelFactory)
            CatalogueScreen(
                viewModel = catalogueViewModel,
                onTitleClick = { titleId ->
                    navController.navigate(Screen.detailRoute(titleId))
                }
            )
        }

        composable(
            route = Screen.Detail,
            arguments = listOf(
                navArgument("titleId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val titleId = backStackEntry.arguments?.getString("titleId").orEmpty()
            val detailViewModel: TitleDetailViewModel = viewModel(factory = viewModelFactory)
            TitleDetailScreen(
                titleId = titleId,
                viewModel = detailViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
