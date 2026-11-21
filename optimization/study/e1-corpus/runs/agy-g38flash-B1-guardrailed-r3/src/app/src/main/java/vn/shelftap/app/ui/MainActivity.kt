package vn.shelftap.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import vn.shelftap.app.ShelfTapApplication
import vn.shelftap.app.ui.catalogue.CatalogueScreen
import vn.shelftap.app.ui.catalogue.CatalogueViewModel
import vn.shelftap.app.ui.detail.TitleDetailScreen
import vn.shelftap.app.ui.detail.TitleDetailViewModel
import vn.shelftap.app.ui.navigation.Screen
import vn.shelftap.app.ui.theme.ShelfTapTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as ShelfTapApplication).container

        setContent {
            ShelfTapTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Catalogue.route
                    ) {
                        composable(route = Screen.Catalogue.route) {
                            val catalogueViewModel: CatalogueViewModel = viewModel(
                                factory = CatalogueViewModel.provideFactory(
                                    repository = appContainer.catalogueRepository
                                )
                            )

                            CatalogueScreen(
                                viewModel = catalogueViewModel,
                                onTitleClick = { titleId ->
                                    navController.navigate(Screen.Detail.createRoute(titleId))
                                }
                            )
                        }

                        composable(
                            route = Screen.Detail.route,
                            arguments = listOf(
                                navArgument(Screen.Detail.ARG_TITLE_ID) {
                                    type = NavType.StringType
                                }
                            )
                        ) { backStackEntry ->
                            val titleId = backStackEntry.arguments?.getString(Screen.Detail.ARG_TITLE_ID) ?: ""
                            val detailViewModel: TitleDetailViewModel = viewModel(
                                key = titleId,
                                factory = TitleDetailViewModel.provideFactory(
                                    titleId = titleId,
                                    repository = appContainer.catalogueRepository
                                )
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
            }
        }
    }
}
