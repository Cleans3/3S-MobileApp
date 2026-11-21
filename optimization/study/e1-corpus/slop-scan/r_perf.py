"""Performance / memory rules P01-P11, P13 (P12 is cited-only, R14)."""
import re
from slopscan import rule, F, theme_file
from ktlite import split_top, named_arg, modifier_arg

LAZY = r"LazyColumn|LazyRow|LazyVerticalGrid|LazyHorizontalGrid|LazyVerticalStaggeredGrid|LazyHorizontalStaggeredGrid"
SAFE_LAMBDA = {"remember", "rememberSaveable", "derivedStateOf", "LaunchedEffect", "produceState", "DisposableEffect", "SideEffect",
               "snapshotFlow", "launch", "async", "withContext", "collect", "collectLatest", "clickable", "combinedClickable",
               "toggleable", "pointerInput", "drawBehind", "graphicsLayer", "offset", "layout", "onGloballyPositioned", "rememberCoroutineScope",
               "rememberUpdatedState", "viewModelScope", "scope", "map", "flow", "stateIn", "combine", "update", "run", "also", "apply", "let"}
IO_OK = re.compile(r"Dispatchers\.(IO|Default)|\bdispatcher\b|ioDispatcher|defaultDispatcher|Dispatchers\.Unconfined", re.I)
LAZY_CONTENT = r"items|itemsIndexed|item"

def lazy_calls(k): return k.calls(LAZY)

def region(k, c): return (c.lstart if c.lstart is not None else c.start), c.end

def content_calls(k, c):
    a, b = region(k, c)
    inner = [n for n in k.calls(LAZY) if a < n.start < b]          # nested lazy lists own their items
    out = []
    for x in k.calls(LAZY_CONTENT):
        if a < x.start < b and not any(n.start < x.start < n.end for n in inner) and k.skel[x.start - 1:x.start] != ".":
            out.append(x)
    return out

@rule("P01")
def p01(p):
    """Lazy list items()/itemsIndexed() call without a key (named, or a positional lambda as the 2nd argument)."""
    out = []
    for k in p.kt:
        for c in lazy_calls(k):
            for x in content_calls(k, c):
                if x.name == "item": continue
                args = k.args(x)
                if named_arg(args, "key") is not None: continue
                parts = split_top(args)
                if len(parts) >= 2 and "{" in parts[1] and "=" not in parts[1].split("{")[0]: continue
                out.append(F("P01", k.path, k.line(x.start), f"{x.name}({args.strip()[:60]}) in {c.name} has no key"))
    return out

@rule("P02")
def p02(p):
    """Lazy list with >= 2 content blocks (or a type-switching items lambda) and no contentType anywhere in it."""
    out = []
    for k in p.kt:
        for c in lazy_calls(k):
            cc = content_calls(k, c)
            body = k.code[region(k, c)[0]:c.end]
            if "contentType" in body: continue
            switching = any(x.name != "item" and re.search(r"\bwhen\s*\(.*?\)\s*\{[^}]*\bis\s+\w+", k.body(x), re.S) for x in cc)
            if len(cc) >= 2 or switching:
                out.append(F("P02", k.path, k.line(c.start), f"{c.name} with {len(cc)} content blocks{' (type switch)' if switching else ''}, no contentType"))
    return out

UNSTABLE_BASE = {"List", "Set", "Map", "Collection", "Iterable", "MutableList", "MutableSet", "MutableMap", "ArrayList", "HashMap", "HashSet",
                 "LinkedList", "Array", "Sequence", "IntArray", "LongArray", "MutableCollection"}

def unstable_classes(p):
    out = set()
    for k in p.kt:
        for m in re.finditer(r"((?:@\w+(?:\([^)]*\))?\s*)*)(?:data\s+)?class\s+(\w+)\s*(?:<[^>]*>)?\s*\(", k.skel):
            if re.search(r"@(Immutable|Stable)\b", k.code[max(0, m.start() - 120):m.end()]): continue
            o = m.end() - 1; c = k.match.get(o)
            if c is not None and re.search(r"\bvar\s+\w+", k.code[o:c]): out.add(m.group(2))
    return out

@rule("P03")
def p03(p):
    """Composable parameter whose type is a Kotlin collection interface/array or a project class with a var property (not @Stable/@Immutable)."""
    unstable = unstable_classes(p); out = []
    for k in p.kt:
        for f in k.funs:
            if not f.composable or f.name.startswith("Preview") or "@Preview" in k.code[max(0, f.start - 200):f.start]: continue
            for prm in split_top(f.params):
                m = re.match(r"\s*(?:@\w+\s+)*(?:vararg\s+)?(\w+)\s*:\s*([^=]+?)\s*(?:=.*)?$", prm, re.S)
                if not m: continue
                name, typ = m.group(1), " ".join(m.group(2).split())
                if "->" in typ or re.search(r"Immutable|Persistent|ImmutableList", typ): continue
                base = re.match(r"([\w.]+)", typ.lstrip("("))
                base = base.group(1).split(".")[-1] if base else ""
                if base in UNSTABLE_BASE or base in unstable:
                    out.append(F("P03", k.path, k.line(f.start), f"fun {f.name}({name}: {typ}) unstable parameter"))
    return out

HEAVY = (r"\.(sorted|sortedBy|sortedByDescending|sortedDescending|sortedWith|filter|filterNot|groupBy|distinct|distinctBy)\s*[({]|"
         r"\b(Regex|SimpleDateFormat|DecimalFormat)\s*\(|\.toRegex\s*\(|DateTimeFormatter\.ofPattern|NumberFormat\.get\w+|"
         r"Json\.decodeFromString|\.fromJson\s*\(|Gson\s*\(\s*\)|\.readText\s*\(")

def in_unsafe_composition(k, pos):
    """pos is inside a composable fun body and not inside any deferred/event/side-effect lambda."""
    f = k.enclosing_fun(pos)
    if f is None or not f.composable: return None
    for b in k.enclosing(pos):
        if b[0] <= f.bstart: continue
        lab = b[2].lstrip("=")
        if lab in SAFE_LAMBDA or lab.startswith(("on", "remember", "LaunchedEffect")) or b[2].startswith("="):
            return None
    return f

@rule("P04")
def p04(p):
    """Sort/filter/group/regex/date-format/JSON parsing executed in a composable body outside remember/derivedStateOf/effects/event lambdas."""
    out = []
    for k in p.kt:
        for m in re.finditer(HEAVY, k.skel):
            f = in_unsafe_composition(k, m.start())
            if f: out.append(F("P04", k.path, k.line(m.start()), f"{k.snippet(m.start(), n=90)} in composable {f.name} without remember"))
    return out

def is_modifier_lambda(k, b):
    """Trailing lambda of a call on a Modifier chain (Modifier.foo(x) { ... }): runs at layout/draw/semantics time, never in composition."""
    hdr = k.code[max(0, b[0] - 240):b[0]]
    m = re.search(r"\.\s*\w+\s*(\([^{}]*\))?\s*$", hdr)
    return bool(m and re.search(r"(Modifier|\bmodifier)\b[^{};]*$", hdr[:m.start()]))

DEFER_OK = {"derivedStateOf", "snapshotFlow", "LaunchedEffect", "produceState", "graphicsLayer", "offset", "drawBehind", "layout", "DisposableEffect", "SideEffect"}
@rule("P05")
def p05(p):
    """Scroll position (firstVisibleItemIndex/ScrollOffset, scrollState.value) read directly in composition, not via derivedStateOf/snapshotFlow/deferred lambda."""
    out = []
    for k in p.kt:
        for m in re.finditer(r"\.(firstVisibleItemIndex|firstVisibleItemScrollOffset)\b|\bscrollState\.value\b", k.skel):
            f = k.enclosing_fun(m.start())
            if f is None or not f.composable: continue
            if any(b[2].lstrip("=") in DEFER_OK or b[2].lstrip("=").startswith("on") or is_modifier_lambda(k, b) for b in k.enclosing(m.start()) if b[0] > f.bstart): continue
            out.append(F("P05", k.path, k.line(m.start()), f"{k.snippet(m.start() - 20, n=90)} read in composition of {f.name}"))
    return out

BOUNDED = re.compile(r"\b(size|width|height|fillMax\w*|aspectRatio|weight|sizeIn|requiredSize|requiredWidth|requiredHeight|defaultMinSize)\b")
@rule("P06")
def p06(p):
    """Image decoded/loaded without bounds or with caching off: manual BitmapFactory decode without inSampleSize, AsyncImage with no size-bounding modifier, Size.ORIGINAL, cache disabled."""
    out = []
    for k in p.kt:
        if "BitmapFactory" in k.skel and "inSampleSize" not in k.skel:
            for m in re.finditer(r"BitmapFactory\.decode\w+", k.skel):
                out.append(F("P06", k.path, k.line(m.start()), f"{m.group(0)} without inSampleSize (full-resolution decode)"))
        for c in k.calls(r"AsyncImage|SubcomposeAsyncImage"):
            mod = modifier_arg(k.args(c))
            if mod is None or (not BOUNDED.search(mod) and not re.fullmatch(r"modifier|Modifier", mod.strip())):
                out.append(F("P06", k.path, k.line(c.start), f"{c.name} without a size-bounding modifier (decodes at original size)"))
        for m in re.finditer(r"Size\.ORIGINAL|CachePolicy\.DISABLED|DiskCacheStrategy\.NONE|skipMemoryCache\(\s*true|SIZE_ORIGINAL", k.skel):
            out.append(F("P06", k.path, k.line(m.start()), f"{m.group(0)}: size/cache protection switched off"))
    return out

BLOCKING = (r"\bURL\s*\([^)]*\)\s*\.(readText|readBytes|openStream|openConnection)|\.openConnection\s*\(|"
            r"newCall\s*\([^)]*\)\s*\.execute\s*\(|\bFile\s*\([^)]*\)\s*\.(readText|readBytes|writeText|writeBytes|readLines)|"
            r"\bassets\.open\s*\(|\bopenRawResource\s*\(|Thread\.sleep\s*\(")
@rule("P07")
def p07(p):
    """runBlocking / allowMainThreadQueries / blocking network, file or asset I/O outside withContext(IO|Default), Thread, executor or worker."""
    out = []
    for k in p.kt:
        for m in re.finditer(r"\brunBlocking\b|allowMainThreadQueries\s*\(", k.skel):
            if re.search(r"fun\s+main\s*\(", k.skel[max(0, m.start() - 200):m.start()]): continue
            if k.skel[k.skel.rfind("\n", 0, m.start()) + 1:m.start()].lstrip().startswith("import "): continue
            cls = list(re.finditer(r"\b(class|object)\s+\w+[^{=]*\{", k.skel[:m.start()]))
            if cls and re.search(r"Interceptor|Authenticator|Worker|Service|Application", cls[-1].group(0)): continue    # runs on a background thread by contract
            if re.search(r"^(fun\s+)?(intercept|authenticate|doWork)\b", k.code[max(0, m.start() - 120):m.start()].split("fun ")[-1]): continue
            out.append(F("P07", k.path, k.line(m.start()), f"{m.group(0)} blocks the calling (main) thread"))
        def safe_at(pos):
            for b in k.enclosing(pos):
                lab = b[2].lstrip("=")
                hdr = k.code[max(0, b[0] - 90):b[0]]
                if lab in ("withContext", "launch", "async", "flowOn", "produceIn") and IO_OK.search(hdr): return True
                if lab in ("Thread", "thread", "execute", "submit", "schedule", "doWork", "runInBackground"): return True
                if lab == "flow" and "flowOn(" in k.skel: return True
            return False
        for m in re.finditer(BLOCKING, k.skel):
            if re.search(r"Worker\b|Service\b", k.skel[:3000]) and re.search(r"class\s+\w*(Worker|Service)", k.skel): continue
            cls = list(re.finditer(r"\b(class|object)\s+\w+[^{=]*\{", k.skel[:m.start()]))
            if cls and re.search(r"Interceptor|Authenticator", cls[-1].group(0)): continue      # OkHttp calls these off the main thread by contract
            safe = safe_at(m.start())
            if not safe:
                f = k.enclosing_fun(m.start())
                if f is not None and not f.composable:
                    sites = [c.start() for c in re.finditer(r"(?<![\w.])" + re.escape(f.name) + r"\s*\(", k.skel) if k.skel[max(0, c.start() - 4):c.start()] != "fun "]
                    if sites and all(safe_at(s) for s in sites): continue                           # helper only called under IO/thread in this file
            if not safe:
                out.append(F("P07", k.path, k.line(m.start()), f"{k.snippet(m.start(), n=90)} not under an IO dispatcher/thread"))
    return out

# ---- P08: visual-effect tax in list items; R13 'decorative-effect density'
EFFECTS = [
    ("blur", re.compile(r"\.blur\s*\(|\bRenderEffect\b|createBlurEffect")),
    ("gradient", re.compile(r"Brush\.(linearGradient|verticalGradient|horizontalGradient|radialGradient|sweepGradient)")),
    ("graphicsLayer-effect", re.compile(r"graphicsLayer\b[^\n;]{0,160}?\b(alpha|renderEffect|shadowElevation)\b")),
    ("infinite-animation", re.compile(r"rememberInfiniteTransition|infiniteRepeatable")),
]
ELEV = re.compile(r"(?:\.shadow\s*\(\s*(?:elevation\s*=\s*)?|shadowElevation\s*=\s*|defaultElevation\s*=\s*|\belevation\s*=\s*)(\d+(?:\.\d+)?)\.dp")

def effects_in(k, a, b):
    seg = k.skel[a:b]; hits = []
    for name, rx in EFFECTS:
        for m in rx.finditer(seg): hits.append((name, a + m.start(), m.group(0)))
    for m in ELEV.finditer(k.skel, a, b):
        if float(m.group(1)) > 2: hits.append(("shadow>2dp", m.start(), m.group(0)))
    return hits

def gradient_vals(p):
    names = set()
    for k in p.kt:
        for m in re.finditer(r"\bval\s+(\w+)\s*(?::[^=]+)?=\s*Brush\.\w*[gG]radient", k.skel): names.add(m.group(1))
    return names

def item_scopes(p):
    """(Kt, start, end, label) of every item lambda in a lazy list plus the bodies of composables they call (transitively, depth 4)."""
    seen, scopes, stack = set(), [], []
    for k in p.kt:
        for c in k.calls(LAZY):
            for x in content_calls(k, c):
                if x.lstart is not None: stack.append((k, x.lstart, x.lend, f"{c.name}.{x.name}", 0))
    while stack:
        k, a, b, lab, d = stack.pop()
        if (k.path, a) in seen: continue
        seen.add((k.path, a)); scopes.append((k, a, b, lab))
        if d >= 4: continue
        for m in re.finditer(r"(?<![\w.])([A-Z]\w*)\s*\(", k.skel[a:b]):
            for kk, f in p.funs.get(m.group(1), []):
                stack.append((kk, f.bstart, f.bend, f.name, d + 1))
    return scopes

@rule("P08")
def p08(p):
    """Decorative effects (blur/RenderEffect, shadow or elevation > 2dp, gradient brush, graphicsLayer alpha/renderEffect, infinite animation) inside a lazy-list item or a composable it calls. Also computes the R13 effect-density metrics."""
    gv = gradient_vals(p); out, seen = [], set(); n_item = 0
    scopes = item_scopes(p)
    for k, a, b, lab in scopes:
        n_item += 1
        hits = effects_in(k, a, b)
        for g in gv:
            for m in re.finditer(r"\b" + re.escape(g) + r"\b", k.skel[a:b]): hits.append(("gradient", a + m.start(), g))
        for name, pos, txt in hits:
            if (k.path, pos) in seen: continue
            seen.add((k.path, pos))
            out.append(F("P08", k.path, k.line(pos), f"{name}: {k.snippet(pos, n=70)} in list item scope {lab}"))
    eff_total = 0; comp = 0; screens = 0
    for k in p.kt:
        for f in k.funs:
            if f.composable:
                comp += 1; screens += bool(re.search(r"(Screen|Page|Route)$", f.name))
        eff_total += len(effects_in(k, 0, len(k.skel)))
        for g in gv: eff_total += len(re.findall(r"\b" + re.escape(g) + r"\b", k.skel))
    m = {"effects_item": len(out), "item_scopes": n_item, "effect_density_item": round(len(out) / max(1, n_item), 3),
         "effects_total": eff_total, "composables": comp, "screens": screens, "effect_density_screen": round(eff_total / max(1, screens), 3)}
    return out, m

EMITS = re.compile(r"(?<![\w.])[A-Z]\w*\s*[({]")      # a composable (or other constructor-style) call in the loop body
ITER = re.compile(r"\.forEach(?:Indexed)?\s*\{|\bfor\s*\(\s*\(?\w+[\w, ]*\)?\s+in\s+([^)]+)\)|\brepeat\s*\(")
STATIC_ITER = re.compile(r"listOf\(|arrayOf\(|\.entries|values\(\)|\bindices\b|\d+\s*(until|\.\.)\s*\d+|tabs|chips|subjects|authors|genres|filters|options|steps|fields|menu|destinations|sections|labels|categories", re.I)

@rule("P09")
def p09(p):
    """Column/Row with verticalScroll/horizontalScroll that iterates a collection to emit children (or forEach inside a single lazy item) instead of using a lazy list."""
    out = []
    for k in p.kt:
        for c in k.calls(r"Column|Row"):
            mod = modifier_arg(k.args(c)) or ""
            if not re.search(r"verticalScroll|horizontalScroll", mod) or c.lstart is None: continue
            for m in ITER.finditer(k.skel, c.lstart, c.lend):
                ctx = k.skel[max(m.start() - 60, c.lstart):m.start()]
                if STATIC_ITER.search(ctx + k.skel[m.start():m.end() + 40]) or re.search(r"(?<![\w])[A-Z][A-Z0-9_]+(\.[A-Z][A-Z0-9_]+)*\.forEach|repeat\s*\(", k.skel[max(0, m.start() - 40):m.end() + 12]): continue
                if not EMITS.search(k.skel[m.end():m.end() + 700]): continue                       # a loop that calls no composable emits no children (validation, bookkeeping)
                out.append(F("P09", k.path, k.line(m.start()), f"{c.name}+scroll iterates ({k.snippet(m.start() - 30, n=80)})"))
        for c in k.calls(LAZY):
            for x in content_calls(k, c):
                if x.name == "item" and x.lstart is not None:
                    for m in ITER.finditer(k.skel, x.lstart, x.lend):
                        ctx = k.skel[max(m.start() - 60, x.lstart):m.start()]
                        if STATIC_ITER.search(ctx + k.skel[m.start():m.end() + 40]): continue
                        out.append(F("P09", k.path, k.line(m.start()), f"forEach/for inside a single lazy item() ({k.snippet(m.start() - 30, n=80)})"))
    return out

APP_PLUGIN = re.compile(r"[\"']com\.android\.application[\"']|plugins\.android\.application\b")

def app_modules(p):
    """Module scripts that apply the Android application plugin themselves; `apply false` (root script) and convention plugins (release type set elsewhere, not visible) do not count."""
    for path, t in p.gradle:
        if not path.endswith(("build.gradle", "build.gradle.kts")): continue
        if any(APP_PLUGIN.search(ln) and not re.search(r"apply\s+false", ln) for ln in t.splitlines()): yield path, t

@rule("P10")
def p10(p):
    """Application module whose release build type does not enable R8 minification (and, when it does, resource shrinking)."""
    out = []
    for path, t in app_modules(p):
        on = re.search(r"(isMinifyEnabled|minifyEnabled)\s*(=\s*|\s)true|optimization\s*\{[^}]*enable\s*=\s*true", t)
        rel = re.search(r"\b(release|getByName\(\"release\"\)|create\(\"release\"\))\s*\{", t)
        shrink = re.search(r"(isShrinkResources|shrinkResources)\s*(=\s*|\s)true", t)
        line = lambda rx: t[:rx.start()].count("\n") + 1 if rx else 1
        if not on:
            out.append(F("P10", path, line(rel), "release build type has no isMinifyEnabled = true (R8 off by default)" if rel else "no release build type: R8 minification off by default"))
        elif not shrink:
            out.append(F("P10", path, line(on), "minify enabled but isShrinkResources not enabled"))
    return out

@rule("P11")
def p11(p):
    """No Baseline Profile: no baselineprofile plugin and no baseline-prof.txt anywhere in the project."""
    has_plugin = re.search(r"androidx\.baselineprofile|baselineprofile", p.gradletext)
    has_file = any(path.endswith("baseline-prof.txt") for path, _ in p.other)
    apps = list(app_modules(p))
    if apps and not has_plugin and not has_file:
        return [F("P11", apps[0][0], 1, "no baselineprofile plugin and no baseline-prof.txt")]
    return []

SEARCHV = r"(query|searchText|searchQuery|keyword|searchTerm|searchString|term|needle)"
INDEXED = re.compile(r"@Fts[45]|USING\s+fts[45]|\bMATCH\s*[:'\"?]|invertedIndex|\bTrie\b|\btrie\b|prefixIndex|searchIndex|Lucene|MiniSearch")
@rule("P13")
def p13(p):
    """Search implemented as a linear in-memory filter or an unindexed SQL LIKE (no FTS/index), or a per-keystroke remote search with no debounce."""
    out = []; indexed = bool(INDEXED.search(p.alltext)); debounced = bool(re.search(r"\bdebounce\w*\b", p.alltext))   # flatMapLatest/collectLatest cancel, they do not debounce
    for k in p.kt:
        for c in k.calls(r"filter|filterNot|any|firstOrNull|find"):
            body = (k.body(c) + k.args(c))
            if re.search(r"(contains|startsWith|indexOf|matches|equals)\s*\(", body) and re.search(r"\b" + SEARCHV + r"\b", body + k.code[max(0, c.start - 200):c.start], re.I):
                if not indexed:
                    out.append(F("P13", k.path, k.line(c.start), f"linear {c.name} over the list on the search query, no index; debounce={'yes' if debounced else 'no'}"))
        if not indexed:
            for m in re.finditer(r"@Query\s*\(([^)]*\bLIKE\b[^)]*)\)", k.code, re.S):
                out.append(F("P13", k.path, k.line(m.start()), f"unindexed LIKE query, no FTS table; debounce={'yes' if debounced else 'no'}"))
        if not debounced:
            for m in re.finditer(r"onValueChange\s*=\s*\{[^}]*\b(search|query|load\w*|fetch\w*)\w*\s*\(", k.skel):
                out.append(F("P13", k.path, k.line(m.start()), "search triggered on every keystroke, no debounce anywhere in the project"))
    return out
