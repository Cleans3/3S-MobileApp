package vn.shelftap.app.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.ui.catalogue.CatalogueScreen
import vn.shelftap.app.ui.catalogue.CatalogueViewModel
import vn.shelftap.app.ui.detail.TitleDetailScreen
import vn.shelftap.app.ui.detail.TitleDetailViewModel
import vn.shelftap.app.ui.search.SearchScreen
import vn.shelftap.app.ui.search.SearchViewModel

object Routes {
    const val CATALOGUE = "catalogue"
    const val SEARCH = "search"
    const val DETAIL = "title/{titleId}"

    fun detail(titleId: String): String = "title/${Uri.encode(titleId)}"
}

@Composable
fun ShelfTapNavHost(repository: CatalogueRepository) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.CATALOGUE) {
        composable(Routes.CATALOGUE) {
            CatalogueScreen(
                viewModel = viewModel(factory = CatalogueViewModel.factory(repository)),
                onTitleClick = { id -> navController.navigate(Routes.detail(id)) },
                onSearchClick = { navController.navigate(Routes.SEARCH) },
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                viewModel = viewModel(factory = SearchViewModel.factory(repository)),
                onBack = { navController.popBackStack() },
                onTitleClick = { id -> navController.navigate(Routes.detail(id)) },
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("titleId") { type = NavType.StringType }),
        ) { entry ->
            val titleId = entry.arguments?.getString("titleId").orEmpty()
            TitleDetailScreen(
                viewModel = viewModel(factory = TitleDetailViewModel.factory(repository, titleId)),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
