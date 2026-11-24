package shelftap.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import shelftap.android.ui.theme.LocalEffectTier
import shelftap.android.ui.theme.Motion
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
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
import shelftap.android.ui.icons.LucideMorphs
import shelftap.android.ui.icons.MorphIcon
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import shelftap.android.R
import shelftap.android.api.ApiException
import shelftap.android.api.ErrorCode
import shelftap.android.api.TitleDetail
import shelftap.android.api.TitleSummary
import shelftap.android.data.Repository
import shelftap.android.ui.theme.CoverSize
import shelftap.android.ui.theme.GlassSize
import shelftap.android.ui.theme.ShelfTapText
import shelftap.android.ui.theme.Radius
import shelftap.android.ui.theme.Spacing
import shelftap.android.ui.theme.bannerBackground
import shelftap.android.ui.theme.bannerScrim
import shelftap.android.ui.theme.shelfTap
import shelftap.android.ui.theme.rememberRiseIn
import shelftap.android.ui.theme.riseIn

class CatalogViewModel(private val repo: Repository) : ViewModel() {
    data class UiState(
        val query: String = "",
        val items: List<TitleSummary> = emptyList(),
        val page: Int = -1,
        val total: Long = 0,
        val loading: Boolean = false,
        val error: UiText? = null,
        val offline: Boolean = false,
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
        _state.update { it.copy(error = null, offline = false, loading = true) }
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
                _state.update { it.copy(loading = false, error = e.userMessage(), offline = e is ApiException.Network) }
            }
        }
    }
}

private const val SEARCH_DEBOUNCE_MS = 300L

/** Books shown in one strip of the home, and strips shown. */
private const val STRIP_BOOKS = 8
private const val STRIPS = 2
private const val BANNER_BOOKS = 3
private const val BANNER_SCRIM_ALPHA = 0.75f

/** Above this system font scale the banner stacks its text under the covers. */
private const val STACK_FONT_SCALE = 1.3f

/** One strip of the home: a subject and some of its books. */
private data class Strip(val subject: String?, val books: List<TitleSummary>)

/**
 * The home groups the books of the first page by their first subject: the two biggest groups (at least two books) become strips.
 * With no such group it shows one strip of the first books. (The API has no "new" or "most borrowed" and no subject filter, so
 * this is only the page the app already has; a subject strip opens the search for that subject.)
 */
private fun stripsOf(items: List<TitleSummary>): List<Strip> {
    val bySubject = items.filter { it.subjects.isNotEmpty() }.groupBy { it.subjects.first() }.filterValues { it.size >= 2 }
    val strips = bySubject.entries.sortedByDescending { it.value.size }.take(STRIPS).map { Strip(it.key, it.value.take(STRIP_BOOKS)) }
    return strips.ifEmpty { listOf(Strip(null, items.take(STRIP_BOOKS))) }
}

@Composable
fun CatalogScreen(onOpen: (String) -> Unit) {
    val vm = appViewModel { CatalogViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    var listAll by rememberSaveable { mutableStateOf(false) }
    // Search while typing, after a pause, so a burst of keystrokes is one request; the older query is cancelled in the ViewModel.
    LaunchedEffect(input) {
        if (input.trim() != state.query) {
            delay(SEARCH_DEBOUNCE_MS)
            vm.search(input)
        }
    }
    // Typing or "see all" turns the home into the list of results; back returns to the home.
    val showList = listAll || input.isNotBlank()
    BackHandler(enabled = showList) {
        input = ""
        listAll = false
    }

    val rise = rememberRiseIn()
    val searchLabel = stringResource(R.string.catalog_search_label)
    val search: @Composable () -> Unit = {
        SearchPill(
            value = input, onValueChange = { input = it }, label = searchLabel, placeholder = stringResource(R.string.catalog_search_placeholder), onSearch = { vm.search(input) },
            modifier = Modifier.padding(vertical = Spacing.s),
        )
    }
    if (showList) {
        // The results: the search field stays on top (below the bar) over the rows, which run under it and under the bottom button.
        // The list's own top padding is the bar plus the field, so the first row starts below both and nothing is counted twice.
        // The field folds away to its middle while the rows scroll down and opens out to both sides on a scroll up (and while typing).
        // It changes state after a short run in one direction (no following the finger), then plays a 260 ms animation read in the draw phase.
        // With a screen reader on it stays. Folded away, it is not on screen, not touchable and not read out.
        val insets = LocalPageInsets.current
        val searchBlock = GlassSize.search + Spacing.s * 2
        val slopPx = with(LocalDensity.current) { Spacing.xl.toPx() }
        val screenReader = rememberTouchExploration()
        val tier = LocalEffectTier.current
        var shown by remember { mutableStateOf(true) }
        val open = remember { Animatable(1f) } // 1 open .. 0 folded
        LaunchedEffect(input, screenReader) { shown = true }
        LaunchedEffect(shown, tier) { open.animateTo(if (shown) 1f else 0f, Motion.spec(tier, Motion.MENU_MS)) }
        val onScreen by remember { derivedStateOf { open.value > 0f } }
        val follow = remember(slopPx, screenReader) {
            var run = 0f
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val dy = available.y
                    if (!screenReader && dy != 0f) {
                        if (dy * run < 0f) run = 0f
                        run += dy
                        if (run < -slopPx) shown = false else if (run > slopPx) shown = true
                    }
                    return Offset.Zero // only watching: the list scrolls as it would
                }
            }
        }
        Box(Modifier.fillMaxSize().nestedScroll(follow)) {
            CatalogList(state, onOpen, rise, onLoadMore = vm::loadMore, onRetry = vm::retry, topExtra = searchBlock)
            if (onScreen) {
                Box(Modifier.align(Alignment.TopCenter).graphicsLayer { alpha = open.value }) {
                    FadeBand(insets.top + searchBlock, fromTop = true)
                    Box(
                        Modifier.padding(top = insets.top, start = GlassSize.page, end = GlassSize.page)
                            .drawWithContent {
                                val side = size.width * (1f - open.value) / 2f
                                clipRect(left = side, right = size.width - side) { this@drawWithContent.drawContent() }
                            },
                    ) { search() }
                }
            }
        }
    } else {
        // The home: greeting and search scroll with the page, so everything passes under the top bar as in the mockup.
        val header: @Composable () -> Unit = {
            Column(Modifier.padding(top = Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(stringResource(R.string.catalog_hello), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.shelfTap.ink3)
                Text(stringResource(R.string.catalog_headline), style = ShelfTapText.greeting)
            }
            search()
        }
        CatalogHome(state, onOpen, onSubject = { input = it }, onAll = { listAll = true }, onRetry = vm::retry, header = header)
    }
}

/** Banner, strips by subject, and a way to the full list. */
@Composable
private fun CatalogHome(
    state: CatalogViewModel.UiState, onOpen: (String) -> Unit, onSubject: (String) -> Unit, onAll: () -> Unit, onRetry: () -> Unit,
    header: @Composable () -> Unit,
) {
    val top = LocalPageInsets.current.top
    when {
        state.items.isEmpty() && state.loading -> Column { Spacer(Modifier.height(top)); Column(Modifier.padding(horizontal = GlassSize.page)) { header() }; SkeletonList(rows = 6, padded = false) }
        state.items.isEmpty() && state.error != null -> Column { Spacer(Modifier.height(top)); Column(Modifier.padding(horizontal = GlassSize.page)) { header() }; ErrorState(state.error!!, state.offline, onRetry) }
        state.items.isEmpty() -> Column { Spacer(Modifier.height(top)); Column(Modifier.padding(horizontal = GlassSize.page)) { header() }; EmptyState(R.string.catalog_empty, R.string.catalog_empty_hint) }
        else -> {
            val strips = remember(state.items) { stripsOf(state.items) }
            LazyColumn(Modifier.fillMaxSize(), state = rememberPreparedListState(), contentPadding = pagePadding(GlassSize.page, Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
                item { Column { header() } }
                item {
                    val first = strips.first()
                    Banner(first.subject, first.books.take(BANNER_BOOKS), onClick = { first.subject?.let(onSubject) ?: onAll() })
                }
                items(strips, key = { it.subject ?: "" }) { strip ->
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                        SectionHeader(
                            title = strip.subject ?: stringResource(R.string.catalog_library_strip),
                            actionLabel = stringResource(R.string.see_all),
                            onAction = { strip.subject?.let(onSubject) ?: onAll() },
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                            items(strip.books, key = { it.titleId }) { book ->
                                BookCard(
                                    title = book.title, author = book.authors.joinToString(", "),
                                    status = if (book.copiesAvailable > 0) stringResource(R.string.catalog_available, book.copiesAvailable) else stringResource(R.string.catalog_all_on_loan),
                                    onClick = { onOpen(book.titleId) },
                                    cover = { Cover(book.coverUrl, book.title, Modifier.fillMaxSize(), shape = RectangleShape, large = true) },
                                )
                            }
                        }
                    }
                }
                item { SecondaryButton(onClick = onAll, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.catalog_see_all_books)) } }
            }
        }
    }
}

/**
 * The theme banner: a few covers, a scrim at the foot for the white text, the subject, an arrow. The whole banner is one button.
 * With an enlarged system font the text no longer fits under the covers, so it moves to its own area below them and the banner grows,
 * instead of drawing over the covers' titles.
 */
@Composable
private fun Banner(subject: String?, books: List<TitleSummary>, onClick: () -> Unit) {
    val theme = stringResource(R.string.catalog_theme)
    val name = subject ?: stringResource(R.string.catalog_library_strip)
    val stacked = LocalDensity.current.fontScale > STACK_FONT_SCALE
    val frame = Modifier.fillMaxWidth().pressable(RoundedCornerShape(Radius.banner), onClick, row = true).background(bannerBackground)
        .semantics(mergeDescendants = true) { contentDescription = "$theme: $name" }
    val covers: @Composable () -> Unit = {
        Row(Modifier.padding(start = Spacing.xl, top = Spacing.xl), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            books.forEach { book ->
                Cover(
                    book.coverUrl, book.title,
                    Modifier.size(GlassSize.bannerCoverWidth, GlassSize.bannerCoverHeight).shadow(GlassSize.shadow, RoundedCornerShape(Radius.cover)),
                    shape = RoundedCornerShape(Radius.cover), large = true,
                )
            }
        }
    }
    val label: @Composable (Modifier) -> Unit = { m ->
        Row(m.fillMaxWidth().padding(start = Spacing.xl, end = Spacing.l, bottom = Spacing.l), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(theme, style = MaterialTheme.typography.labelLarge, color = Color.White)
                Text(name, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            GlassArrow()
        }
    }
    if (stacked) {
        Column(frame) {
            covers()
            Spacer(Modifier.height(Spacing.m))
            label(Modifier)
        }
    } else {
        Box(frame.height(GlassSize.bannerHeight)) {
            covers()
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(GlassSize.scrimHeight)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, bannerScrim.copy(alpha = BANNER_SCRIM_ALPHA)))),
            )
            label(Modifier.align(Alignment.BottomCenter))
        }
    }
}

/** Every book of the search, one row each, with paging. Rows are solid and carry no blur (plan section 7). */
@Composable
private fun CatalogList(
    state: CatalogViewModel.UiState, onOpen: (String) -> Unit, rise: Boolean, onLoadMore: () -> Unit, onRetry: () -> Unit, topExtra: Dp,
) {
    val insets = LocalPageInsets.current
    LazyColumn(Modifier.fillMaxSize(), state = rememberPreparedListState(), contentPadding = PaddingValues(top = insets.top + topExtra, bottom = insets.bottom)) {
        itemsIndexed(state.items, key = { _, item -> item.titleId }) { index, item ->
            TitleRow(item, onClick = { onOpen(item.titleId) }, modifier = Modifier.riseIn(index, rise))
            HorizontalDivider()
            if (index == state.items.lastIndex) LaunchedEffect(state.items.size) { onLoadMore() }
        }
        item {
            when {
                state.loading -> SkeletonList(rows = if (state.items.isEmpty()) 6 else 1, padded = false) // first page: full skeleton; later pages: one row
                state.error != null -> ErrorState(state.error!!, state.offline, onRetry, Modifier.fillMaxWidth())
                state.items.isEmpty() -> EmptyState(R.string.catalog_empty, R.string.catalog_empty_hint, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun TitleRow(item: TitleSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().pressable(RectangleShape, onClick, role = null, row = true).padding(Spacing.l), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Cover(item.coverUrl, item.title, Modifier.size(width = CoverSize.rowWidth, height = CoverSize.rowHeight), shape = RoundedCornerShape(Radius.tile))
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

/** Cover addresses that failed to load in this run: asked for again only after the app restarts, not every time a card scrolls back into view. */
private val failedCovers: MutableSet<String> = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap())

/**
 * The cover: its colour and title underneath (so a book without an image is never an empty box), the image on top when there is one.
 * An address that failed once is not requested again while scrolling: with an unreachable image host (measured on the phone, where the
 * development data points at the emulator's 10.0.2.2) every card scrolling into view started a request that could only fail.
 */
@Composable
internal fun Cover(coverUrl: String?, title: String, modifier: Modifier, shape: Shape = RoundedCornerShape(Radius.tile), large: Boolean = false) {
    val baseUrl = LocalContainer.current.baseUrl
    val url = coverUrl?.let { baseUrl.resolve(it)?.toString() } // absolute or server-relative
    Box(modifier.clip(shape)) {
        TextCover(title, Modifier.fillMaxSize(), shape = shape, large = large)
        if (url != null && url !in failedCovers) {
            AsyncImage(
                model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                onError = { failedCovers.add(url) },
            )
        }
    }
}

class TitleViewModel(private val repo: Repository, private val titleId: String) : ViewModel() {
    data class UiState(
        val title: Load<TitleDetail> = Load.Loading, val busy: Boolean = false, val message: UiText? = null, val error: UiText? = null,
        /** Reserved now, or the server said ALREADY_RESERVED: the button stays off and says why. */
        val reserved: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(title = Load.Loading) }
        viewModelScope.launch {
            val result = try { Load.Ready(repo.title(titleId)) } catch (e: ApiException) { e.toFailed() }
            _state.update { it.copy(title = result) }
        }
    }

    fun reserve() {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null, error = null) }
        viewModelScope.launch {
            try {
                repo.reserve(titleId)
                _state.update { it.copy(busy = false, reserved = true, message = uiText(R.string.title_reserved)) }
            } catch (e: ApiException) {
                _state.update {
                    it.copy(busy = false, error = e.userMessage(), reserved = it.reserved || (e is ApiException.Http && e.code == ErrorCode.ALREADY_RESERVED))
                }
            }
        }
    }
}
