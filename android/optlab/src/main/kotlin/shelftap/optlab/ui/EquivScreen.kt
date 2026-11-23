package shelftap.optlab.ui

import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import shelftap.optlab.corpus.p13.FIRST_PAGE
import shelftap.optlab.corpus.p13.PAGING_SEARCH_SQL
import shelftap.optlab.corpus.p13.likePattern
import shelftap.optlab.data.BookDb
import shelftap.optlab.data.BookGenerator

/**
 * P13 semantic check, not a measurement: for every typed prefix of [query], compares B's result set (FTS4, uncapped and the first page of 120 in title order, as P13c's B returns it) with
 * the corpus A (the LIKE '%q%' query of the generated app, uncapped) and with the author-constructed naive token-prefix search (h).
 * One logcat line per prefix, tag STLAB; the harness turns them into p13_equivalence.csv.
 */
@Composable
fun EquivScreen(query: String) {
    val ctx = LocalContext.current
    val status by produceState("running") {
        value = withContext(Dispatchers.Default) {
            val all = BookGenerator.all()
            val db = BookDb.openWithTitles(ctx)
            for (i in 1..query.length) {
                val q = query.take(i)
                val h = naiveSearch(all, q).map { it.id }
                val like = db.rows(PAGING_SEARCH_SQL, likePattern(q)).map { it.id }
                val likePage = db.rows(PAGING_SEARCH_SQL + " LIMIT " + FIRST_PAGE, likePattern(q)).map { it.id }
                val b = db.search(q, 0).map { it.id }
                val bPage = db.search(q, FIRST_PAGE, titleOrder = true).map { it.id }
                val hPage = naiveSearch(all, q).sortedWith(compareBy({ it.title.lowercase() }, { it.id })).take(FIRST_PAGE).map { it.id }
                fun jaccard(x: List<Int>, y: List<Int>): Double {
                    val sx = x.toSet(); val sy = y.toSet()
                    val union = (sx + sy).size
                    return if (union == 0) 1.0 else (sx intersect sy).size.toDouble() / union
                }
                Log.i("STLAB", "p13eq i=$i q=\"$q\" a=${like.size} h=${h.size} b=${b.size} bpage=${bPage.size} " +
                    "jac_full=${jaccard(like, b)} jac_page=${jaccard(likePage, bPage)} jac_h_full=${jaccard(h, b)} jac_h_page=${jaccard(hPage, bPage)}")
            }
            "equiv done"
        }
    }
    Text(status)
}
