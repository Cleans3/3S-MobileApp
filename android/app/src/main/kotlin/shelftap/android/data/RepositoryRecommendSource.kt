package shelftap.android.data

import shelftap.android.api.HistoryItem
import shelftap.android.api.Loan
import shelftap.android.api.Me
import shelftap.android.api.Reservation
import shelftap.android.api.TitleDetail
import shelftap.android.api.TitleSummary
import shelftap.android.recommend.BookInfo
import shelftap.android.recommend.RecommendSource
import shelftap.android.recommend.SearchHits

/** The suggestions read the server through the same repository as the screens, so they share its error handling and its memory of listed books. */
class RepositoryRecommendSource(private val repo: Repository) : RecommendSource {
    override suspend fun me(): Me = repo.me()
    override suspend fun loans(): List<Loan> = repo.loans()
    override suspend fun reservations(): List<Reservation> = repo.reservations()
    override suspend fun history(): List<HistoryItem> = repo.history()

    override suspend fun book(titleId: String): BookInfo = repo.seenTitle(titleId)?.toInfo() ?: repo.title(titleId).toInfo()

    override suspend fun search(query: String): SearchHits {
        val page = repo.titles(query, 0)
        return SearchHits(page.items.map { it.toInfo() }, page.total)
    }

    override fun knownCatalogSize(): Long? = repo.catalogSize
}

private fun TitleSummary.toInfo() = BookInfo(titleId, title, authors, subjects, copiesAvailable, coverUrl)
private fun TitleDetail.toInfo() = BookInfo(titleId, title, authors, subjects, copiesAvailable, coverUrl)
