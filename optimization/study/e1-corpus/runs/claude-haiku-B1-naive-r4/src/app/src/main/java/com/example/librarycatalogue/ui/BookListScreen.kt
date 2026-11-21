package com.example.librarycatalogue.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.librarycatalogue.R
import com.example.librarycatalogue.data.Book

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookListScreen(viewModel: BookListViewModel) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val books = viewModel.books.collectAsLazyPagingItems()
    val listState = rememberLazyListState()

    // Show the top of the new result set whenever the search text changes.
    LaunchedEffect(query) { listState.scrollToItem(0) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            SearchField(query = query, onQueryChange = viewModel::onQueryChange)
            BookListContent(books = books, listState = listState)
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        singleLine = true,
        label = { Text(stringResource(R.string.search_hint)) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        trailingIcon = {
            if (query.isNotEmpty()) {
                TextButton(onClick = { onQueryChange("") }) {
                    Text(stringResource(R.string.clear))
                }
            }
        },
    )
}

@Composable
private fun BookListContent(books: LazyPagingItems<Book>, listState: LazyListState) {
    val refresh = books.loadState.refresh
    when {
        refresh is LoadState.Loading && books.itemCount == 0 -> CenteredBox {
            CircularProgressIndicator()
        }

        refresh is LoadState.Error && books.itemCount == 0 -> CenteredBox {
            RetryContent(onRetry = { books.retry() })
        }

        books.itemCount == 0 -> CenteredBox {
            Text(stringResource(R.string.no_books))
        }

        else -> LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(
                count = books.itemCount,
                key = books.itemKey { it.id },
            ) { index ->
                books[index]?.let { BookRow(book = it) }
            }

            val append = books.loadState.append
            if (append is LoadState.Loading) {
                item { ProgressRow() }
            } else if (append is LoadState.Error) {
                item { RetryContent(onRetry = { books.retry() }) }
            }
        }
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun ProgressRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun RetryContent(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.error_loading))
        Row {
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}
