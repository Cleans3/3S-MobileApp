package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import vn.shelftap.app.ui.ShelfTapApp
import vn.shelftap.app.ui.theme.ShelfTapTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as ShelfTapApplication).container
        setContent {
            ShelfTapTheme {
                ShelfTapApp(container = container)
            }
        }
    }
}
