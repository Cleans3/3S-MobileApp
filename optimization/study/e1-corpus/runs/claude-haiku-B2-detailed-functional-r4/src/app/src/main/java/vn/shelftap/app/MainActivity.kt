package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import vn.shelftap.app.session.AppViewModel
import vn.shelftap.app.ui.ShelfTapRoot
import vn.shelftap.app.ui.theme.ShelfTapTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShelfTapTheme {
                val auth = (application as ShelfTapApp).container.auth
                val app: AppViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { AppViewModel(auth) }
                    },
                )
                ShelfTapRoot(app)
            }
        }
    }
}
