package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.shelftap.app.ui.ShelfTapTheme
import vn.shelftap.app.ui.ShelfTapViewModel
import vn.shelftap.app.ui.ShelfTapViewModelFactory
import vn.shelftap.app.ui.ShelfTapApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as ShelfTapApplication).repository
        setContent {
            ShelfTapTheme {
                val model: ShelfTapViewModel = viewModel(factory = ShelfTapViewModelFactory(repository))
                val state by model.state.collectAsStateWithLifecycle()
                Surface { ShelfTapApp(state, model::onAction) }
            }
        }
    }
}
