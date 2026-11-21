package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import vn.shelftap.app.ui.AccountViewModel
import vn.shelftap.app.ui.AccountViewModelFactory
import vn.shelftap.app.ui.ShelfTapRoot
import vn.shelftap.app.ui.theme.ShelfTapTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AccountViewModel by viewModels {
        AccountViewModelFactory((application as ShelfTapApp).container.accountRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShelfTapTheme {
                ShelfTapRoot(viewModel)
            }
        }
    }
}
