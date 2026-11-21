package vn.shelftap.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import vn.shelftap.app.R
import vn.shelftap.app.data.Title

@Composable
fun ShelfTapApp(state: ShelfTapState, dispatch: (ShelfTapAction) -> Unit) {
    if (state.selected == null) CatalogueScreen(state, dispatch) else DetailScreen(state.selected, dispatch)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogueScreen(state: ShelfTapState, dispatch: (ShelfTapAction) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState, state.titles.size, state.hasMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { if (it >= state.titles.lastIndex - 5 && state.hasMore) dispatch(ShelfTapAction.LoadMore) }
    }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.catalogue), fontWeight = FontWeight.Bold) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.query, onValueChange = { dispatch(ShelfTapAction.QueryChanged(it)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.search_catalogue)) }, leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = if (state.query.isNotEmpty()) ({ IconButton(onClick = { dispatch(ShelfTapAction.QueryChanged("")) }) { Icon(Icons.Default.Clear, stringResource(R.string.clear_search)) } }) else null,
                singleLine = true, shape = RoundedCornerShape(14.dp)
            )
            if (state.offline) Text(stringResource(R.string.offline_catalogue), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp))
            if (!state.loading && state.titles.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_books_found)) }
            else LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.fillMaxSize()) {
                items(state.titles, key = { it.id }, contentType = { "title" }) { title -> TitleRow(title) { dispatch(ShelfTapAction.OpenTitle(title.id)) } }
                if (state.loading) item(key = "loading", contentType = { "loading" }) { Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            }
        }
    }
}

@Composable
private fun TitleRow(title: Title, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(116.dp).then(Modifier.clickable(onClick = onClick)), verticalAlignment = Alignment.CenterVertically) {
        Cover(title, Modifier.size(76.dp, 104.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Text(title.authors.joinToString(", "), style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            Text(title.year.toString(), style = MaterialTheme.typography.bodySmall)
            Badge(containerColor = if (title.copiesAvailable > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer) { Text(if (title.copiesAvailable > 0) stringResource(R.string.available) else stringResource(R.string.all_copies_out), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)) }
        }
    }
}

@Composable
private fun Cover(title: Title, modifier: Modifier) {
    if (title.coverUrl != null) AsyncImage(model = title.coverUrl, contentDescription = stringResource(R.string.cover_description, title.title), contentScale = ContentScale.Crop, modifier = modifier)
    else Box(modifier, contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_cover), style = MaterialTheme.typography.labelSmall) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreen(title: Title, dispatch: (ShelfTapAction) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(title.title, maxLines = 1) }, navigationIcon = { IconButton(onClick = { dispatch(ShelfTapAction.Back) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } }) }) { padding ->
        LazyColumn(contentPadding = PaddingValues(bottom = 28.dp), modifier = Modifier.fillMaxSize().padding(padding)) {
            item(key = "cover") { Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) { Cover(title, Modifier.size(220.dp, 330.dp)) } }
            item(key = "info") { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(title.authors.joinToString(", "), style = MaterialTheme.typography.titleMedium)
                Text("${stringResource(R.string.year)}: ${title.year}")
                Text("${stringResource(R.string.isbn)}: ${title.isbn}")
                Text("${stringResource(R.string.copies_available)}: ${title.copiesAvailable}")
                Text(stringResource(R.string.subjects), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(title.subjects, key = { index, _ -> index }, contentType = { _, _ -> "subject" }) { _, subject ->
                        AssistChip(onClick = {}, label = { Text(subject) })
                    }
                }
            } }
        }
    }
}
