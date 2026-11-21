package vn.shelftap.app.data

import vn.shelftap.app.data.local.CatalogueDao
import vn.shelftap.app.data.local.SyncStateEntity
import vn.shelftap.app.data.local.toEntity
import vn.shelftap.app.data.remote.CatalogueApi

/**
 * Downloads the whole catalogue once, page by page. Each page and the progress marker are
 * saved in one transaction, so an interrupted download resumes where it stopped.
 */
class CatalogueSync(
    private val dao: CatalogueDao,
    private val api: CatalogueApi,
) {

    /** Suspends on the caller's dispatcher; the API and Room calls are main-safe. Throws IOException on network failure. */
    suspend fun run() {
        val saved = dao.syncState()
        if (saved?.complete == true) return

        var nextPage = saved?.nextPage ?: 0
        while (true) {
            val page = api.fetchPage(nextPage, DOWNLOAD_PAGE_SIZE)
            nextPage += 1
            val finished = page.items.isEmpty() || nextPage * DOWNLOAD_PAGE_SIZE >= page.total
            dao.upsertPage(
                items = page.items.map { it.toEntity() },
                state = SyncStateEntity(nextPage = nextPage, complete = finished),
            )
            if (finished) return
        }
    }

    companion object {
        /** The API accepts at most 50 items per page. */
        const val DOWNLOAD_PAGE_SIZE = 50
    }
}
