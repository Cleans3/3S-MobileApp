#!/usr/bin/env python3
"""Oracle self-test: the untouched target must show 0 fixed + guards pass; the reference fix must show every issue fixed + guards pass;
each bad variant must trip the guard / extra check it is built for.   Run: selftest.py"""
import csv, pathlib, shutil, sys, tempfile, time
sys.path.insert(0, str(pathlib.Path(__file__).parent))
import oracle

TARGET = pathlib.Path(__file__).resolve().parent.parent / "target"
K = "app/src/main/kotlin/vn/shelftap/slop/"
MF = "app/src/main/AndroidManifest.xml"
FIX = {
    K + "CatalogScreen.kt": [
        ("val shown = vm.books.sortedBy { it.title }.filter { it.title.contains(vm.query, ignoreCase = true) }", "val shown = vm.shown"),
        ("items(shown) { book -> BookRow(book) }", "items(shown, key = { it.id }) { book -> BookRow(book) }"),
        ('    val pulse by rememberInfiniteTransition(label = "pulse")\n        .animateFloat(0.9f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse")\n', ""),
        ("            .shadow(12.dp)\n            .graphicsLayer { alpha = pulse },", "            .shadow(1.dp),"),
        ("Box(Modifier.size(72.dp).blur(4.dp))", "Box(Modifier.size(72.dp))"),
        (".size(Size.ORIGINAL)", ".size(216)"),
    ],
    K + "CatalogViewModel.kt": [
        ("books = runBlocking { repo.load() }", "viewModelScope.launch { books = repo.load() }"),
        ("    fun onQueryChange(value: String) {", "    val shown: List<Book> get() = books.filter { it.title.contains(query, ignoreCase = true) } // debounce(300) in the real fix\n\n    fun onQueryChange(value: String) {"),
    ],
    K + "TokenStore.kt": [('putString("token", token)', 'putString("token", seal(token)) // AndroidKeyStore AES-GCM')],
    K + "Net.kt": [("Level.BODY", "Level.NONE")],
    K + "CatalogRepository.kt": [('"http://10.0.2.2:8080/"', '"https://api.shelftap.example/"')],
    K + "MainActivity.kt": [('    @Deprecated("Deprecated in Java")\n    override fun onBackPressed() {\n        // ask before leaving the catalog\n        super.onBackPressed()\n    }\n', "    // back handling through OnBackPressedCallback\n")],
    "app/build.gradle.kts": [("isMinifyEnabled = false", "isMinifyEnabled = true")],
    MF: [('android:allowBackup="true"', 'android:allowBackup="false"'), ('android:usesCleartextTraffic="true"', 'android:usesCleartextTraffic="false"'),
         ('android:name=".ShareActivity"\n            android:exported="true"', 'android:name=".ShareActivity"\n            android:exported="false"')],
}

def apply(root, edits):
    for rel, reps in edits.items():
        p = root / rel; t = p.read_text()
        for a, b in reps:
            if a not in t: sys.exit(f"selftest anchor missing in {rel}: {a[:60]!r}")
            t = t.replace(a, b)
        p.write_text(t)

def fresh():
    base = pathlib.Path("/mnt/01DC198BE9192500/github/Schooldev/3S-study-work/tmp/beta-f2-4"); base.mkdir(parents=True, exist_ok=True)
    d = pathlib.Path(tempfile.mkdtemp(prefix="e5oracle-", dir=base)); shutil.copytree(TARGET, d / "t"); return d / "t"

def strip_trust(root):
    p = root / (K + "Net.kt"); t = p.read_text()
    a = t.index("        val trustAll = object"); b = t.index("        OkHttpClient.Builder()")
    t = (t[:a] + t[b:]).replace("            .sslSocketFactory(ssl.socketFactory, trustAll)\n", "").replace("            .hostnameVerifier { _, _ -> true }\n", "")
    p.write_text(t)

def check(name, root, expect_fixed=None, expect_fail=(), expect_extra=()):
    t0 = time.time(); r = oracle.run(root)
    fixed = {k for k, v in r["issues"].items() if v["status"].startswith("fixed")}
    ok = (expect_fixed is None or fixed == set(expect_fixed)) and all(f in " ".join(r["regressions"]) for f in expect_fail) and all(e in " ".join(r["extra_weakenings"]) for e in expect_extra)
    if expect_fixed is not None and not expect_fail and not expect_extra: ok = ok and not r["regressions"]
    print(("PASS" if ok else "FAIL"), name, f"fixed={len(fixed)}/{r['total']} regressions={r['regressions']} ({time.time()-t0:.2f}s)", flush=True)
    if not ok: print("   status:", {k: v["status"] for k, v in r["issues"].items()}); sys.exit(1)

ALL = {i["issue_id"] for i in csv.DictReader(open(TARGET.parent / "ground_truth.csv")) if i["tier"] == "seeded"}
check("untouched target", TARGET, expect_fixed=())
ref = fresh(); apply(ref, FIX); strip_trust(ref); check("reference fix", ref, expect_fixed=ALL)
b1 = fresh(); apply(b1, {MF: [FIX[MF][1]]}); check("cleartext off but base URL still http", b1, expect_fail=("G7",))
b2 = fresh(); (b2 / (K + "ShareActivity.kt")).unlink(); check("ShareActivity.kt deleted", b2, expect_fail=("G6",))
b3 = fresh(); apply(b3, {K + "TokenStore.kt": [("fun save(", "fun store(")]}); check("TokenStore API renamed", b3, expect_fail=("G4",))
b4 = fresh(); apply(b4, {K + "CatalogViewModel.kt": [("books = runBlocking { repo.load() }", "StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build()); books = runBlocking { repo.load() }")]}); check("StrictMode relaxed", b4, expect_extra=("X4",))
print("oracle selftest OK")
