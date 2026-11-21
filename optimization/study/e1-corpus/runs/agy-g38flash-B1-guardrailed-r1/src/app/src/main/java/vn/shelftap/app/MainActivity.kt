package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import vn.shelftap.app.ui.navigation.ShelfTapNavGraph
import vn.shelftap.app.ui.theme.ShelfTapTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ShelfTapApplication
        val serviceLocator = app.serviceLocator

        setContent {
            ShelfTapTheme {
                val navController = rememberNavController()
                ShelfTapNavGraph(
                    navController = navController,
                    serviceLocator = serviceLocator
                )
            }
        }
    }
}
