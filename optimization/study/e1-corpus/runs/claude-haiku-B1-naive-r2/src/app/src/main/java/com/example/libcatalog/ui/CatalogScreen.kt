package com.example.libcatalog.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.libcatalog.R
import com.example.libcatalog.data.Book

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(viewModel: CatalogViewModel) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val books = viewModel.books.collectAsLazyPagingItems()
    val listState = rememberLazyListState()

    // A new search starts from the top of the results.
    LaunchedEffect(query) { listState.scrollToItem(0) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.catalogue_title)) })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            SearchField(query = query, onQueryChange = viewModel::onQueryChange)
            BookList(books = books, listState = listState)
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        trailingIcon = {
            if (query.isNotEmpty()) {
                TextButton(onClick = { onQueryChange("") }) {
                    Text(stringResource(R.string.clear_search))
                }
            }
        },
    )
}

@Composable
private fun BookList(books: LazyPagingItems<Book>, listState: LazyListState) {
    val refresh = books.loadState.refresh

    when {
        books.itemCount == 0 && refresh is LoadState.Loading -> CenteredBox {
            CircularProgressIndicator()
        }

        books.itemCount == 0 && refresh is LoadState.Error -> CenteredBox {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.error_loading))
                TextButton(onClick = { books.retry() }) {
                    Text(stringResource(R.string.retry))
                }
            }
        }

        books.itemCount == 0 && refresh is LoadState.NotLoading -> CenteredBox {
            Text(stringResource(R.string.no_results))
        }

        else -> LazyColumn(state = listState) {
            items(
                count = books.itemCount,
                key = books.itemKey { it.id },
            ) { index ->
                // Placeholders are disabled in the Pager, so the item is normally present.
                books[index]?.let { BookRow(book = it) }
            }

            when (books.loadState.append) {
                is LoadState.Loading -> item { FooterSpinner() }
                is LoadState.Error -> item {
                    CenteredBox {
                        TextButton(onClick = { books.retry() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun FooterSpinner() {
    CenteredBox { CircularProgressIndicator() }
}
