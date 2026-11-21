package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import vn.shelftap.app.ui.navigation.AppNavigation
import vn.shelftap.app.ui.theme.ShelfTapTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as ShelfTapApplication
        val repository = app.repository

        setContent {
            ShelfTapTheme {
                AppNavigation(repository = repository)
            }
        }
    }
}
