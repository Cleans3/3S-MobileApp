package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import vn.shelftap.app.ui.ShelfTapNavHost
import vn.shelftap.app.ui.theme.ShelfTapTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = (application as ShelfTapApplication).container.repository
        setContent {
            ShelfTapTheme {
                ShelfTapNavHost(repository = repository)
            }
        }
    }
}
