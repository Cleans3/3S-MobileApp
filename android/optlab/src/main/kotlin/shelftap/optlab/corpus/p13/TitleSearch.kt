package shelftap.optlab.corpus.p13

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import shelftap.optlab.data.Book

// Ported from claude-haiku-B1-detailed-functional-r1 (app/src/main/java/vn/shelftap/app/...):
//   data/local/TitleDao.kt  pagingSearch: the SQL below, unindexed LIKE '%pattern%' on searchText (the P13 finding); Room's :pattern is a ? placeholder.
//   data/CatalogueRepository.kt  likePattern: unchanged.
//   ui/SearchScreen.kt  SearchViewModel.results: debounce(200).distinctUntilChanged().flatMapLatest, unchanged except that the Pager/Room
//   PagingSource is replaced by a query on Dispatchers.IO (Room runs on its query executor) returning the first page (Paging initial load = 3 x pageSize 40).

/** SQLite LIKE is case-sensitive for non-ASCII, so the pattern is lowercased; wildcard characters are escaped with '!'. */
internal fun likePattern(query: String): String =
    query.trim().lowercase()
        .replace("!", "!!")
        .replace("%", "!%")
        .replace("_", "!_")

const val PAGING_SEARCH_SQL =
    "SELECT * FROM titles WHERE searchText LIKE '%' || ? || '%' ESCAPE '!' " +
        "ORDER BY title COLLATE NOCASE, id"

/** First page of Paging: PagingConfig(pageSize = 40) loads 3 x 40 on the first call. */
const val FIRST_PAGE = 120

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
fun corpusResults(query: Flow<String>, run: (pattern: String) -> List<Book>): Flow<List<Book>> = query
    .debounce(200)
    .distinctUntilChanged()
    .flatMapLatest { text -> flow { emit(withContext(Dispatchers.IO) { run(likePattern(text)) }) } }
