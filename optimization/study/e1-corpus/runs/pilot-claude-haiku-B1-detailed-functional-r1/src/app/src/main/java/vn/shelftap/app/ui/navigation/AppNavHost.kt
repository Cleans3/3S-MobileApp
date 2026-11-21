package vn.shelftap.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.ui.catalogue.CatalogueScreen
import vn.shelftap.app.ui.detail.TitleDetailScreen
import vn.shelftap.app.ui.search.SearchScreen

object Routes {
    const val CATALOGUE = "catalogue"
    const val SEARCH = "search"
    const val DETAIL = "title/{id}"

    fun detail(id: String): String = "title/${Uri.encode(id)}"
}

@Composable
fun AppNavHost(repository: CatalogueRepository) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.CATALOGUE) {
        composable(Routes.CATALOGUE) {
            CatalogueScreen(
                repository = repository,
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onOpenTitle = { id -> navController.navigate(Routes.detail(id)) },
            )
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onOpenTitle = { id -> navController.navigate(Routes.detail(id)) },
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            TitleDetailScreen(
                repository = repository,
                titleId = id,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
