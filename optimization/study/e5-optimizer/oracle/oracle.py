#!/usr/bin/env python3
"""E5 independent oracle: judges a copy of e5-optimizer/target by static checks, never by the fixer's own claims.

  oracle.py DIR [--json]   -> per seeded issue: present | fixed | fixed-new-issue ; guards (regression checks) ; extra weakenings
Issue ids are the contract's taxonomy ids (P01.. S07, D01). 'fixed-new-issue' = fixed with a deprecated/weaker replacement (counted fixed, flagged).
Seeded ground truth lives in ground_truth.csv (id, file, what). Decoys (must NOT be reported / changed): RecentRow keyed LazyRow, Stats() with remember+derivedStateOf, BuildConfig.DEBUG-guarded Log in TokenStore.
"""
import json, pathlib, re, sys

def _read(root, rel):
    p = pathlib.Path(root) / rel
    return p.read_text() if p.exists() else ""

def _kt(root):
    """Kotlin sources with import lines removed (an unused leftover import must not count as usage)."""
    return {p: "\n".join(l for l in p.read_text().splitlines() if not l.lstrip().startswith("import ")) for p in sorted(pathlib.Path(root, "app/src/main/kotlin").rglob("*.kt"))}

def balanced(src, i):
    """src[i] is an opening bracket; return index just past its matching close (ignores brackets in strings crudely)."""
    pairs = {"(": ")", "{": "}", "[": "]"}; open_c = src[i]; close_c = pairs[open_c]; d = 0
    for j in range(i, len(src)):
        if src[j] == open_c: d += 1
        elif src[j] == close_c:
            d -= 1
            if d == 0: return j + 1
    return len(src)

def calls(src, name):
    """All 'name(' ... ')' argument texts (balanced), with the 1-based line of the call."""
    out = []
    for m in re.finditer(r"(?<![\w.])" + re.escape(name) + r"\s*\(", src):
        i = src.index("(", m.start()); out.append((src.count("\n", 0, m.start()) + 1, src[i + 1:balanced(src, i) - 1]))
    return out

def strip_blocks(src, names):
    """Remove 'name(...)' / 'name { ... }' blocks (remember, derivedStateOf ...) so they do not count as composition-time work."""
    for n in names:
        while True:
            m = re.search(r"(?<![\w.])" + n + r"\s*[({]", src)
            if not m: break
            i = m.end() - 1; src = src[:m.start()] + src[balanced(src, i):]
    return src

def block_after(src, pattern):
    m = re.search(pattern, src)
    if not m: return ""
    i = src.find("{", m.end())
    return src[i:balanced(src, i)] if i >= 0 else ""

def run(root):
    kt = _kt(root); allkt = "\n".join(kt.values())
    screen = next((t for p, t in kt.items() if p.name == "CatalogScreen.kt"), "")
    vm = next((t for p, t in kt.items() if p.name == "CatalogViewModel.kt"), "")
    net = next((t for p, t in kt.items() if p.name == "Net.kt"), "")
    store = next((t for p, t in kt.items() if p.name == "TokenStore.kt"), "")
    main = next((t for p, t in kt.items() if p.name == "MainActivity.kt"), "")
    repo = next((t for p, t in kt.items() if p.name == "CatalogRepository.kt"), "")
    mf = _read(root, "app/src/main/AndroidManifest.xml"); gr = _read(root, "app/build.gradle.kts")
    nsc = "\n".join(p.read_text() for p in pathlib.Path(root, "app/src/main/res/xml").glob("*.xml")) if pathlib.Path(root, "app/src/main/res/xml").exists() else ""
    iss = {}
    def put(i, status, ev): iss[i] = {"status": status, "evidence": ev}

    # P01 LazyColumn items without key
    bad = []
    for m in re.finditer(r"LazyColumn", allkt):
        i = allkt.find("{", m.end())
        if i < 0: continue
        for fn in ("items", "itemsIndexed"):
            bad += [a for _, a in calls(allkt[i:balanced(allkt, i)], fn) if "key" not in a]
    put("P01", "present" if bad else "fixed", "LazyColumn items() without key" if bad else "keys present / list replaced")
    # P04 heavy work in composition
    body = strip_blocks(block_after(screen, r"fun\s+CatalogScreen\b[^{]*"), ["remember", "derivedStateOf", "produceState"])
    h = re.search(r"\.(sortedBy|sortedWith|sorted|filter)\b", body)
    put("P04", "present" if h else "fixed", "sort/filter in composition outside remember" if h else "moved out of composition / remembered")
    # P06 full-resolution image decode
    put("P06", "present" if "Size.ORIGINAL" in allkt else "fixed", "Size.ORIGINAL" if "Size.ORIGINAL" in allkt else "sized request")
    # P07 blocking on main
    put("P07", "present" if "runBlocking" in allkt else "fixed", "runBlocking in app code" if "runBlocking" in allkt else "no runBlocking")
    # P08 visual-effect tax in list item
    item = block_after(screen, r"fun\s+BookRow\b[^{]*") or screen
    eff = []
    if re.search(r"rememberInfiniteTransition|infiniteRepeatable", item): eff.append("infinite animation")
    if re.search(r"\.blur\(|RenderEffect|BlurEffect", item): eff.append("blur")
    if any(int(x) > 2 for x in re.findall(r"\.shadow\(\s*(?:elevation\s*=\s*)?(\d+)", item)): eff.append("shadow>2dp")
    put("P08", "present" if eff else "fixed", ", ".join(eff) or "no effect modifiers in list item")
    # P13 search without debounce/index
    uses_deb = re.search(r"debounce\(|delay\(|flatMapLatest|collectLatest|mapLatest|[Ii]ndex|FTS|fts", screen + vm + repo)
    put("P13", "fixed" if uses_deb else "present", "debounce/index found" if uses_deb else "filter on every keystroke, no debounce or index")
    # P10 release without R8
    rel = block_after(gr, r"release\s*")
    put("P10", "fixed" if re.search(r"isMinifyEnabled\s*=\s*true", rel) else "present", "release minify " + ("on" if "isMinifyEnabled = true" in rel else "off"))
    # S01 token in plaintext prefs
    if re.search(r"AndroidKeyStore|Cipher|MasterKey|EncryptedSharedPreferences", store):
        put("S01", "fixed-new-issue" if re.search(r"EncryptedSharedPreferences|MasterKey", store) else "fixed", "keystore-backed storage" + (" (deprecated security-crypto, D01)" if re.search(r"EncryptedSharedPreferences|MasterKey", store) else ""))
    else:
        put("S01", "present" if re.search(r'putString\(\s*"token"', store) else "fixed", "plaintext SharedPreferences token")
    # S03 cleartext
    cl = re.search(r'usesCleartextTraffic="true"', mf) or re.search(r'cleartextTrafficPermitted="true"', nsc)
    put("S03", "present" if cl else "fixed", "cleartext permitted in main" if cl else "cleartext off in main")
    # S04 TLS validation disabled
    s4 = re.search(r"checkServerTrusted\([^)]*\)\s*\{\s*\}|hostnameVerifier\s*\{\s*_\s*,\s*_\s*->\s*true|ALLOW_ALL|object\s*:\s*X509TrustManager", allkt)
    put("S04", "present" if s4 else "fixed", "trust-all / hostname verifier always true" if s4 else "default validation")
    # S05 sensitive logging
    lv = [m.start() for m in re.finditer(r"Level\.BODY", allkt)]
    unguarded = [i for i in lv if "BuildConfig.DEBUG" not in allkt[max(0, i - 220):i + 60]]
    put("S05", "present" if unguarded else "fixed", "HttpLoggingInterceptor BODY in release" if unguarded else "body logging removed or debug-only")
    # S06 backup
    s6 = re.search(r'allowBackup="true"', mf) and not re.search(r"dataExtractionRules|fullBackupContent", mf)
    put("S06", "present" if s6 else "fixed", "allowBackup=true without rules" if s6 else "backup off or rules present")
    # S07 exported component without permission
    sa = re.search(r"<activity[^>]*\.ShareActivity[^>]*?/?>", mf, re.S)
    s7 = bool(sa) and 'exported="true"' in sa.group(0) and "android:permission" not in sa.group(0)
    put("S07", "present" if s7 else "fixed", "ShareActivity exported without permission" if s7 else "not exported / permission-protected / removed")
    # D01 deprecated onBackPressed
    d1 = re.search(r"override\s+fun\s+onBackPressed", main)
    put("D01", "present" if d1 else "fixed", "override onBackPressed (deprecated)" if d1 else "back handling via callback")

    def braces_ok(t): return t.count("{") == t.count("}") and t.count("(") == t.count(")")
    g = {}
    g["G1 sources balanced"] = all(braces_ok(t) for t in kt.values())
    g["G2 CatalogViewModel keeps onQueryChange + books"] = "class CatalogViewModel" in vm and "fun onQueryChange(" in vm and re.search(r"\bbooks\b", vm) is not None
    g["G3 CatalogScreen/BookRow/Book intact"] = "fun CatalogScreen" in screen and "fun BookRow" in screen and bool(re.search(r"class Book\(.*?id: String.*?title: String.*?author: String.*?coverUrl: String", "\n".join(t for p, t in kt.items() if p.name == "Book.kt"), re.S))
    g["G4 TokenStore API (save/load/clear)"] = all(f"fun {f}(" in store for f in ("save", "load", "clear"))
    g["G5 launcher MainActivity + INTERNET"] = 'android.intent.action.MAIN' in mf and "android.permission.INTERNET" in mf
    g["G6 ShareActivity kept"] = "class ShareActivity" in "\n".join(kt.values())
    base = re.search(r'baseUrl:\s*String\s*=\s*"([^"]+)"', repo)
    g["G7 cleartext off => base URL https"] = bool(cl) or (base is not None and base.group(1).startswith("https://"))
    g["G8 repository still returns List<Book>"] = bool(re.search(r"fun\s+load\(\)\s*:\s*List<Book>", repo))
    g["G9 search still filters titles by query"] = bool(re.search(r"contains\(\s*(?:vm\.)?query", screen + vm)) or "filter" in (screen + vm) and "query" in (screen + vm)
    extra = []
    if re.search(r'debuggable="true"', mf): extra.append("X1 debuggable")
    if re.search(r"Log\.[dvi]\([^)]*(\$\{?token|\+\s*token|,\s*token\b)", allkt, re.I): extra.append("X2 token logged")
    if len(re.findall(r'exported="true"', mf)) > 2: extra.append("X3 more exported components")
    if re.search(r"permitAll\(\)|permitNetwork\(\)", allkt): extra.append("X4 StrictMode relaxed")
    decoys = []
    if not re.search(r"items\(\s*recent\s*,\s*key\s*=", screen): decoys.append("RecentRow key removed/changed")
    if not re.search(r"derivedStateOf", screen) or "remember(books)" not in screen: decoys.append("Stats remember/derivedStateOf changed")
    if not re.search(r"if\s*\(\s*BuildConfig\.DEBUG\s*\)\s*Log\.d", store): decoys.append("TokenStore debug-guarded Log changed")
    n_fixed = sum(1 for v in iss.values() if v["status"].startswith("fixed"))
    return {"issues": iss, "guards": g, "extra_weakenings": extra, "fixed": n_fixed, "total": len(iss), "regressions": [k for k, v in g.items() if not v] + extra, "decoys_touched": decoys}

if __name__ == "__main__":
    r = run(sys.argv[1])
    if "--json" in sys.argv: print(json.dumps(r, indent=1))
    else:
        for k, v in r["issues"].items(): print(f"{k}: {v['status']:16s} {v['evidence']}")
        print("guards:", {k: v for k, v in r["guards"].items() if not v} or "all pass", "| extra:", r["extra_weakenings"] or "none", f"| fixed {r['fixed']}/{r['total']}")
