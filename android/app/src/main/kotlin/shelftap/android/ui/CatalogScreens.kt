package shelftap.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import shelftap.android.R
import shelftap.android.api.ApiException
import shelftap.android.api.TitleDetail
import shelftap.android.api.TitleSummary
import shelftap.android.data.Repository
import shelftap.android.ui.theme.CoverSize
import shelftap.android.ui.theme.Spacing

class CatalogViewModel(private val repo: Repository) : ViewModel() {
    data class UiState(
        val query: String = "",
        val items: List<TitleSummary> = emptyList(),
        val page: Int = -1,
        val total: Long = 0,
        val loading: Boolean = false,
        val error: UiText? = null,
    ) {
        val canLoadMore get() = !loading && error == null && items.size < total
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private var job: Job? = null

    init {
        search("")
    }

    fun search(query: String) {
        job?.cancel()
        _state.value = UiState(query = query.trim(), loading = true)
        loadPage(0)
    }

    fun retry() {
        _state.update { it.copy(error = null, loading = true) }
        loadPage(_state.value.page + 1)
    }

    fun loadMore() {
        if (_state.value.canLoadMore) {
            _state.update { it.copy(loading = true) }
            loadPage(_state.value.page + 1)
        }
    }

    private fun loadPage(page: Int) {
        job = viewModelScope.launch {
            try {
                val result = repo.titles(_state.value.query, page)
                _state.update { it.copy(items = it.items + result.items, page = page, total = result.total, loading = false, error = null) }
            } catch (e: ApiException) {
                _state.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }
}

@Composable
fun CatalogScreen(onOpen: (String) -> Unit) {
    val vm = appViewModel { CatalogViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(Spacing.l), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            OutlinedTextField(
                value = input, onValueChange = { input = it }, label = { Text(stringResource(R.string.catalog_search_label)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.search(input) }),
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { vm.search(input) }) { Text(stringResource(R.string.catalog_search)) }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(state.items, key = { _, item -> item.titleId }) { index, item ->
                TitleRow(item, onClick = { onOpen(item.titleId) })
                HorizontalDivider()
                if (index == state.items.lastIndex) LaunchedEffect(state.items.size) { vm.loadMore() }
            }
            item {
                when {
                    state.loading -> Spinner()
                    state.error != null -> Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        ErrorText(state.error)
                        Button(onClick = vm::retry) { Text(stringResource(R.string.action_retry)) }
                    }
                    state.items.isEmpty() -> Text(stringResource(R.string.catalog_empty), Modifier.padding(Spacing.l))
                }
            }
        }
    }
}

@Composable
private fun TitleRow(item: TitleSummary, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(Spacing.l), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Cover(item.coverUrl, Modifier.size(width = CoverSize.rowWidth, height = CoverSize.rowHeight))
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Text(item.authors.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
            Text(
                if (item.copiesAvailable > 0) stringResource(R.string.catalog_available, item.copiesAvailable) else stringResource(R.string.catalog_all_on_loan),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun Cover(coverUrl: String?, modifier: Modifier) {
    val baseUrl = LocalContainer.current.baseUrl
    val url = coverUrl?.let { baseUrl.resolve(it)?.toString() } // absolute or server-relative
    if (url != null) AsyncImage(model = url, contentDescription = null, modifier = modifier) else androidx.compose.foundation.layout.Spacer(modifier)
}

class TitleViewModel(private val repo: Repository, private val titleId: String) : ViewModel() {
    data class UiState(val title: Load<TitleDetail> = Load.Loading, val busy: Boolean = false, val message: UiText? = null, val error: UiText? = null)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(title = Load.Loading) }
        viewModelScope.launch {
            val result = try { Load.Ready(repo.title(titleId)) } catch (e: ApiException) { Load.Failed(e.userMessage()) }
            _state.update { it.copy(title = result) }
        }
    }

    fun reserve() {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null, error = null) }
        viewModelScope.launch {
            try {
                repo.reserve(titleId)
                _state.update { it.copy(busy = false, message = uiText(R.string.title_reserved)) }
            } catch (e: ApiException) {
                _state.update { it.copy(busy = false, error = e.userMessage()) }
            }
        }
    }
}

@Composable
fun TitleDetailScreen(titleId: String) {
    val vm = appViewModel(key = "title-$titleId") { TitleViewModel(it.repository, titleId) }
    val state by vm.state.collectAsStateWithLifecycle()
    LoadView(state.title, onRetry = vm::load) { t ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Cover(t.coverUrl, Modifier.size(width = CoverSize.detailWidth, height = CoverSize.detailHeight))
            Text(t.title, style = MaterialTheme.typography.headlineSmall)
            Text(t.authors.joinToString(", "))
            Text(stringResource(R.string.title_isbn, t.isbn))
            if (t.subjects.isNotEmpty()) Text(t.subjects.joinToString(", "))
            Text(stringResource(R.string.title_copies, t.copiesAvailable, t.copiesTotal))
            state.message?.let { Text(it.asString()) }
            ErrorText(state.error)
            Button(onClick = vm::reserve, enabled = !state.busy) { Text(stringResource(if (state.busy) R.string.title_reserving else R.string.title_reserve)) }
        }
    }
}
