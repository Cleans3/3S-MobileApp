package vn.shelftap.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun ShelfTapApp() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "main") {
        composable("main") {
            MainTabs(onOpenTitle = { titleId -> nav.navigate("title/${Uri.encode(titleId)}") })
        }
        composable(
            route = "title/{titleId}",
            arguments = listOf(navArgument("titleId") { type = NavType.StringType }),
        ) { entry ->
            TitleScreen(
                titleId = entry.arguments?.getString("titleId").orEmpty(),
                onBack = { nav.popBackStack() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTabs(onOpenTitle: (String) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(0) }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("ShelfTap") })
        },
    ) { padding ->
        Scaffold(
            modifier = Modifier.padding(padding),
            topBar = {
                TabRow(selectedTabIndex = selected) {
                    Tab(
                        selected = selected == 0,
                        onClick = { selected = 0 },
                        text = { Text("Loans") },
                    )
                    Tab(
                        selected = selected == 1,
                        onClick = { selected = 1 },
                        text = { Text("History") },
                    )
                }
            },
        ) { inner ->
            when (selected) {
                0 -> LoansScreen(onOpenTitle = onOpenTitle, modifier = Modifier.padding(inner))
                else -> HistoryScreen(onOpenTitle = onOpenTitle, modifier = Modifier.padding(inner))
            }
        }
    }
}
