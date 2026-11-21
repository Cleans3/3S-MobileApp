package shelftap.optlab.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import shelftap.optlab.data.BookGenerator

/** P10 / P11: a plain Material 3 home screen with a scrolling list. Nothing factor-specific, so only the build (R8) or the profile differs. */
@Composable
fun StartupScreen() {
    val books = remember { BookGenerator.all(200) }
    FullyDrawn()
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Text("ShelfTap", style = MaterialTheme.typography.headlineMedium)
        Card(Modifier.padding(vertical = 8.dp)) { Text("Tap a book tag to borrow it.", Modifier.padding(16.dp)) }
        Button(onClick = {}) { Text("Scan tag") }
        LazyColumn(Modifier.fillMaxSize()) {
            items(books, key = { it.id }) { ListItem(headlineContent = { Text(it.title) }, supportingContent = { Text(it.author) }) }
        }
    }
}
