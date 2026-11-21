package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import vn.shelftap.app.ui.ShelfTapRoot
import vn.shelftap.app.ui.theme.ShelfTapTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ShelfTapApplication).container
        setContent {
            ShelfTapTheme {
                ShelfTapRoot(container)
            }
        }
    }
}
