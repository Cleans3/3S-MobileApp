#!/usr/bin/env python3
"""Ports corpus code into the optlab app (alpha's A-variant rule, protocol A4): copies the offending file from the generated app,
applies ONLY mechanical changes (package, imports, data-source binding, instrumentation probes), and writes the port diff to ports/<factor>.diff.
Usage: python3 make_ports.py   (reads <repo>/optimization/study/e1-corpus/runs/<run_id>/src/ (or $CORPUS_DIR), writes android/optlab/src/main/kotlin/shelftap/optlab/corpus/ and ports/*.diff)
Every replacement must match exactly once, so a changed corpus file fails loudly."""
import difflib, os, re, sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[3]
CORPUS = Path(os.environ.get("CORPUS_DIR", str(REPO / "optimization/study/e1-corpus/runs")))
OUT = REPO / "android/optlab/src/main/kotlin/shelftap/optlab/corpus"


def croot(run):
    """The run's app sources: <CORPUS_DIR>/<run>/src (repo copy, e1-corpus/runs) or <CORPUS_DIR>/<run> (study-work copy, deleted by the E1 hygiene pass)."""
    p = CORPUS / run
    return p / "src" if (p / "src" / "app").exists() else p


def sub(text, pairs):
    for old, new in pairs:
        if text.count(old) != 1:
            sys.exit(f"anchor not found exactly once ({text.count(old)}): {old[:70]!r}")
        text = text.replace(old, new)
    return text


def write_port(factor, run, src, dst, text, orig=None, note=""):
    orig = orig if orig is not None else (croot(run) / src).read_text()
    (OUT / dst).parent.mkdir(parents=True, exist_ok=True)
    (OUT / dst).write_text(text)
    diff = "".join(difflib.unified_diff(orig.splitlines(True), text.splitlines(True), f"corpus/{run}/{src}", f"optlab/corpus/{dst}"))
    with (HERE / f"{factor}.diff").open("a") as f:
        f.write(f"# {note}\n{diff}\n")


def diff_only(factor, run, src, dst, note):
    orig = (croot(run) / src).read_text()
    text = (OUT / dst).read_text()
    diff = "".join(difflib.unified_diff(orig.splitlines(True), text.splitlines(True), f"corpus/{run}/{src}", f"optlab/corpus/{dst}"))
    with (HERE / f"{factor}.diff").open("a") as f:
        f.write(f"# {note}\n{diff}\n")


def main():
    for p in HERE.glob("*.diff"):
        p.unlink()
    p08()
    p09_p03p04()
    # Ports written by hand (the offending fragment is extracted from a larger file): diff the port against its source file(s) only.
    diff_only("P01", "claude-haiku-B3-detailed-functional-r1", "app/src/main/java/vn/shelftap/app/ui/history/HistoryScreen.kt", "p01/HistoryList.kt",
              "P01 HistoryList.kt: extraction of the LazyColumn + HistoryCard from HistoryScreen.kt (ViewModel, switch, dialog dropped); RecompositionProbe inserted; B (keyed list) added")
    diff_only("P06", "codex-luna-B3-detailed-functional-r1", "app/src/main/java/vn/shelftap/app/MainActivity.kt", "p06/CoverBitmap.kt",
              "P06 CoverBitmap.kt: extraction of the cover-loading fragment of DetailScreen (line 39, minified original reformatted), d?.coverUrl bound to a parameter, Probe lines inserted")
    for src in ("data/local/TitleDao.kt", "data/CatalogueRepository.kt", "ui/SearchScreen.kt"):
        diff_only("P13", "claude-haiku-B1-detailed-functional-r1", "app/src/main/java/vn/shelftap/app/" + src, "p13/TitleSearch.kt",
                  f"P13 TitleSearch.kt vs {src}: only the pagingSearch SQL, likePattern and the SearchViewModel debounce chain are taken from it")
    print("ports written")


# ---------------------------------------------------------------- P08: BookCover.kt + its caller BookCard.kt (rank 1, agy-g38flash-B1-naive-r1)
# A10 (protocol): the first port (BookCover alone in a 72 dp slot) crashed the study emulator; the unmodified original app does not, so the crash was a port
# defect (a slot geometry the corpus never uses). The port now keeps the corpus context: BookCard (the real caller) in a LazyVerticalGrid like CatalogueScreen.
def p08():
    run = "agy-g38flash-B1-naive-r1"
    base = "app/src/main/java/com/example/librarycatalogue/ui/components/"
    cover = sub((croot(run) / (base + "BookCover.kt")).read_text(), [
        ("package com.example.librarycatalogue.ui.components", "package shelftap.optlab.corpus.p08"),
        ("import coil.compose.AsyncImage\nimport coil.request.ImageRequest\n", "import coil3.compose.AsyncImage\nimport coil3.request.ImageRequest\nimport coil3.request.crossfade\n"),
        ("import com.example.librarycatalogue.model.Book\nimport com.example.librarycatalogue.ui.theme.CoverPalettes\n", ""),
    ])
    write_port("P08", run, base + "BookCover.kt", "p08/BookCover.kt", cover,
               note="P08 BookCover.kt: package, Coil 2 -> Coil 3 imports (+ crossfade extension import), Book and CoverPalettes come from Shims.kt in the same package")
    card_src = (croot(run) / (base + "BookCard.kt")).read_text()
    card = sub(card_src, [
        ("package com.example.librarycatalogue.ui.components", "package shelftap.optlab.corpus.p08"),
        ("import com.example.librarycatalogue.model.Book\nimport com.example.librarycatalogue.ui.theme.AvailableGreen\nimport com.example.librarycatalogue.ui.theme.UnavailableRed\n", ""),
    ])
    write_port("P08", run, base + "BookCard.kt", "p08/BookCard.kt", card,
               note="P08 BookCard.kt (the caller of BookCover, unchanged): package; Book and the two availability colours come from Shims.kt")
    # B (tuned): the same card with the cover replaced by a flat box of the same slot (aspect 0.68, palette colour): no shadow, no gradient overlay, no canvas drawing.
    flat = sub(card, [
        ("fun BookCard(", "fun BookCardFlat("),
        ("""            BookCover(
                book = book,
                modifier = Modifier.fillMaxWidth()
            )""", """            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
                    .background(CoverPalettes[(abs(book.coverColorSeed) % CoverPalettes.size).toInt()].first)
            )"""),
        ("import androidx.compose.foundation.layout.Spacer\n", "import androidx.compose.foundation.layout.Spacer\nimport androidx.compose.foundation.layout.aspectRatio\n"),
        ("import androidx.compose.ui.unit.dp\n", "import androidx.compose.ui.unit.dp\nimport kotlin.math.abs\n"),
    ])
    write_port("P08", run, base + "BookCard.kt", "p08/BookCardFlat.kt", flat, orig=card,
               note="P08 B (tuned): generated from the ported BookCard text; the BookCover call becomes a flat Box of the same slot (aspectRatio 0.68, palette colour), nothing else")


# ------------------------------------------- P09 + P03/P04: BookDetailSheet.kt (agy-g38flash-B3-naive-r1)
def p09_p03p04():
    run, src = "agy-g38flash-B3-naive-r1", "app/src/main/java/com/example/libraryloans/ui/components/BookDetailSheet.kt"
    orig = (croot(run) / src).read_text()
    base = sub(orig, [
        ("package com.example.libraryloans.ui.components", "package shelftap.optlab.corpus.p09"),
        ("import androidx.compose.material.icons.filled.Autorenew\nimport androidx.compose.material.icons.filled.Book\nimport androidx.compose.material.icons.filled.Bookmark\n", ""),
        ("import androidx.compose.material.icons.filled.History\n", ""),
        ("import androidx.compose.material.icons.filled.MenuBook\n", ""),
        ("import androidx.compose.material3.ExperimentalMaterial3Api\n", ""),
        ("import androidx.compose.material3.ModalBottomSheet\n", ""),
        ("import androidx.compose.material3.rememberModalBottomSheetState\n", ""),
        ("import com.example.libraryloans.model.Book\nimport com.example.libraryloans.model.Loan\n", ""),
        ("import com.example.libraryloans.ui.theme.ActiveGreen\nimport com.example.libraryloans.ui.theme.NavyPrimary\nimport com.example.libraryloans.ui.theme.OverdueRed\nimport com.example.libraryloans.ui.theme.Slate100\nimport com.example.libraryloans.ui.theme.Slate600\nimport com.example.libraryloans.ui.theme.Slate800\nimport com.example.libraryloans.ui.viewmodel.LoanWithBook\n", ""),
        ("@OptIn(ExperimentalMaterial3Api::class)\n", ""),
        ("    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)\n", ""),
        ("""    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {""", "    Box(modifier = Modifier.fillMaxWidth()) {  // was ModalBottomSheet(...): inline so TTID and frames see the content"),
        ("                pastLoans.forEach { pastLoan ->\n", "                pastLoans.forEach { pastLoan ->\n                    RecompositionProbe(\"rc.row\")\n"),
        ("import androidx.compose.ui.unit.sp\n", "import androidx.compose.ui.unit.sp\nimport shelftap.optlab.RecompositionProbe\n"),
    ])
    write_port("P09", run, src, "p09/BookDetailSheet.kt", base,
               note="P09 BookDetailSheet.kt: package; model/theme/LoanWithBook/icon symbols come from Shims.kt in the same package; ModalBottomSheet wrapper replaced by a Box (the Column+verticalScroll+forEach construct is untouched); RecompositionProbe inserted in the forEach lambda")

    # --- B for P09: same content, lazily composed history (the tuned fix). Generated from the ported text so header and rows are identical.
    def between(text, a, b):
        i = text.index(a) + len(a)
        return text[i:text.index(b, i)]

    header = between(base, "            // Header with dismiss\n", "            // Past loan history for this book\n")
    hist_head = between(base, "            if (pastLoans.isNotEmpty()) {\n", "                pastLoans.forEach { pastLoan ->\n")
    start = base.index("                pastLoans.forEach { pastLoan ->\n") + len("                pastLoans.forEach { pastLoan ->\n")
    depth, i = 1, start
    while depth:
        depth += {"{": 1, "}": -1}.get(base[i], 0)
        i += 1
    row = base[start:i - 1]
    head_of_file = base[:base.index("@Composable\nfun BookDetailSheet(")]
    b = head_of_file.replace("import androidx.compose.foundation.layout.width\n", "import androidx.compose.foundation.layout.width\nimport androidx.compose.foundation.lazy.LazyColumn\nimport androidx.compose.foundation.lazy.items\n") + f"""/** B of P09: the BookDetailSheet body with the history rendered lazily (LazyColumn + items with key). Generated by make_ports.py from the ported A. */
@Composable
fun BookDetailSheetLazy(
    book: Book,
    loansForBook: List<Loan>,
    onDismiss: () -> Unit,
    onRenewLoan: (LoanWithBook) -> Unit
) {{
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val activeLoan = loansForBook.firstOrNull {{ !it.isReturned }}
    val pastLoans = loansForBook.filter {{ it.isReturned }}

    Box(modifier = Modifier.fillMaxWidth()) {{
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {{
            item {{
                Column {{
{header}                }}
            }}
            if (pastLoans.isNotEmpty()) {{
                item {{
                    Column {{
{hist_head}                    }}
                }}
                items(pastLoans, key = {{ it.id }}) {{ pastLoan ->
{row}                }}
            }}
        }}
    }}
}}
"""
    write_port("P09", run, src, "p09/BookDetailSheetLazy.kt", b, orig=base,
               note="P09 B (tuned): generated from the ported A text; Column+verticalScroll+forEach -> LazyColumn + items(key); header and row bodies copied unchanged")

    # --- B for P03+P04: same eager body, stable parameter + remember (the tuned fix).
    s = sub(base, [
        ("@Composable\nfun BookDetailSheet(", "@Composable\nfun BookDetailSheetStable("),
        ("    loansForBook: List<Loan>,\n", "    loansForBook: LoanList,\n"),
        ('    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")\n', '    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }\n'),
        ("    val activeLoan = loansForBook.firstOrNull { !it.isReturned }\n", "    val activeLoan = remember(loansForBook) { loansForBook.items.firstOrNull { !it.isReturned } }\n"),
        ("    val pastLoans = loansForBook.filter { it.isReturned }\n", "    val pastLoans = remember(loansForBook) { loansForBook.items.filter { it.isReturned } }\n"),
        ("import androidx.compose.runtime.Composable\n", "import androidx.compose.runtime.Composable\nimport androidx.compose.runtime.remember\n"),
    ])
    write_port("P03+P04", run, src, "p09/BookDetailSheetStable.kt", s, orig=base,
               note="P03+P04 B (tuned): generated from the ported A text; stable LoanList parameter (@Immutable, content equality), formatter and filters under remember")


main()
