package vn.shelftap.app.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import vn.shelftap.app.data.local.TitleDao
import vn.shelftap.app.data.local.TitleEntity
import vn.shelftap.app.data.model.Title
import vn.shelftap.app.data.remote.ShelfTapApiService
import vn.shelftap.app.data.sync.CatalogueSyncManager
import vn.shelftap.app.data.sync.SyncStatus

/**
 * Single source of truth repository for catalogue data.
 * Powers offline-first browsing, dynamic search as member types,
 * and background full catalogue synchronization.
 */
class CatalogueRepository(
    private val titleDao: TitleDao,
    private val apiService: ShelfTapApiService,
    private val syncManager: CatalogueSyncManager
) {
    val syncStatus: Flow<SyncStatus> = syncManager.syncStatus
    val localTitleCount: Flow<Int> = titleDao.getTitleCountFlow().distinctUntilChanged()

    /**
     * Provide a Paged stream of Titles for the catalogue list.
     * When [query] is empty or blank, streams all titles sorted alphabetically.
     * When [query] is present, filters titles whose title, author, ISBN, or subject contains the text.
     */
    fun getTitlesStream(query: String): Flow<PagingData<Title>> {
        val trimmed = query.trim()
        val pagingSourceFactory = if (trimmed.isEmpty()) {
            { titleDao.getAllTitlesPaged() }
        } else {
            { titleDao.searchTitlesPaged(trimmed) }
        }

        return Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 20,
                enablePlaceholders = true,
                maxSize = 250
            ),
            pagingSourceFactory = pagingSourceFactory
        ).flow
            .map { pagingData ->
                pagingData.map { it.toDomain() }
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * Retrieve a title by ID. Reads from local Room cache first,
     * with remote API fallback if the title is not found locally.
     */
    suspend fun getTitleById(id: String): Title? = withContext(Dispatchers.IO) {
        val local = titleDao.getTitleById(id)?.toDomain()
        if (local != null) {
            return@withContext local
        }
        // Fallback to network if local cache does not have it yet
        try {
            val remote = apiService.getTitleById(id)
            titleDao.insertTitles(listOf(TitleEntity.fromDomain(remote)))
            remote
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Start downloading the complete catalogue from the API for offline use.
     */
    fun downloadCatalogue() {
        syncManager.startSync()
    }

    /**
     * Cancel an in-progress catalogue download.
     */
    fun cancelDownload() {
        syncManager.cancelSync()
    }

    /**
     * Pre-populate sample titles if database is completely empty,
     * ensuring immediate offline usability out of the box.
     */
    suspend fun seedSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        val count = titleDao.getTitleCount()
        if (count == 0) {
            titleDao.insertTitles(SAMPLE_TITLES.map { TitleEntity.fromDomain(it) })
        }
    }

    companion object {
        val SAMPLE_TITLES = listOf(
            Title(
                id = "title-001",
                isbn = "978-0141439518",
                title = "Pride and Prejudice",
                authors = listOf("Jane Austen"),
                year = 1813,
                subjects = listOf("Classic Literature", "Romance", "19th Century", "British Literature"),
                coverUrl = "https://api.shelftap.example/covers/title-001.jpg",
                copiesAvailable = 4
            ),
            Title(
                id = "title-002",
                isbn = "978-0451524935",
                title = "1984",
                authors = listOf("George Orwell"),
                year = 1949,
                subjects = listOf("Dystopian", "Political Fiction", "Science Fiction", "Classics"),
                coverUrl = "https://api.shelftap.example/covers/title-002.jpg",
                copiesAvailable = 2
            ),
            Title(
                id = "title-003",
                isbn = "978-0061120084",
                title = "To Kill a Mockingbird",
                authors = listOf("Harper Lee"),
                year = 1960,
                subjects = listOf("Classic Literature", "Historical Fiction", "Legal Story", "Southern Gothic"),
                coverUrl = "https://api.shelftap.example/covers/title-003.jpg",
                copiesAvailable = 0
            ),
            Title(
                id = "title-004",
                isbn = "978-0743273565",
                title = "The Great Gatsby",
                authors = listOf("F. Scott Fitzgerald"),
                year = 1925,
                subjects = listOf("Classic Literature", "Jazz Age", "American Fiction"),
                coverUrl = "https://api.shelftap.example/covers/title-004.jpg",
                copiesAvailable = 3
            ),
            Title(
                id = "title-005",
                isbn = "978-0547928227",
                title = "The Hobbit",
                authors = listOf("J.R.R. Tolkien"),
                year = 1937,
                subjects = listOf("Fantasy", "Adventure", "High Fantasy", "Classics"),
                coverUrl = "https://api.shelftap.example/covers/title-005.jpg",
                copiesAvailable = 5
            ),
            Title(
                id = "title-006",
                isbn = "978-0385490818",
                title = "The Handmaid's Tale",
                authors = listOf("Margaret Atwood"),
                year = 1985,
                subjects = listOf("Dystopian", "Feminist Fiction", "Speculative Fiction"),
                coverUrl = "https://api.shelftap.example/covers/title-006.jpg",
                copiesAvailable = 0
            ),
            Title(
                id = "title-007",
                isbn = "978-0345391803",
                title = "The Hitchhiker's Guide to the Galaxy",
                authors = listOf("Douglas Adams"),
                year = 1979,
                subjects = listOf("Science Fiction", "Comedy", "Space Exploration"),
                coverUrl = "https://api.shelftap.example/covers/title-007.jpg",
                copiesAvailable = 6
            ),
            Title(
                id = "title-008",
                isbn = "978-0140283334",
                title = "Things Fall Apart",
                authors = listOf("Chinua Achebe"),
                year = 1958,
                subjects = listOf("African Literature", "Historical Fiction", "Post-colonialism"),
                coverUrl = null,
                copiesAvailable = 1
            ),
            Title(
                id = "title-009",
                isbn = "978-0307277671",
                title = "The Road",
                authors = listOf("Cormac McCarthy"),
                year = 2006,
                subjects = listOf("Post-Apocalyptic", "Literary Fiction", "Survival"),
                coverUrl = "https://api.shelftap.example/covers/title-009.jpg",
                copiesAvailable = 0
            ),
            Title(
                id = "title-010",
                isbn = "978-0262033848",
                title = "Introduction to Algorithms",
                authors = listOf("Thomas H. Cormen", "Charles E. Leiserson", "Ronald L. Rivest", "Clifford Stein"),
                year = 2009,
                subjects = listOf("Computer Science", "Algorithms", "Programming", "Textbooks"),
                coverUrl = null,
                copiesAvailable = 2
            ),
            Title(
                id = "title-011",
                isbn = "978-0132350884",
                title = "Clean Code: A Handbook of Agile Software Craftsmanship",
                authors = listOf("Robert C. Martin"),
                year = 2008,
                subjects = listOf("Software Engineering", "Computer Science", "Programming"),
                coverUrl = "https://api.shelftap.example/covers/title-011.jpg",
                copiesAvailable = 3
            ),
            Title(
                id = "title-012",
                isbn = "978-0679783268",
                title = "Crime and Punishment",
                authors = listOf("Fyodor Dostoevsky"),
                year = 1866,
                subjects = listOf("Classic Literature", "Russian Literature", "Psychological Fiction"),
                coverUrl = "https://api.shelftap.example/covers/title-012.jpg",
                copiesAvailable = 2
            )
        )
    }
}
