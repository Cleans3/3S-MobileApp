"""UI/UX/accessibility rules U01-U05 (U06 cited-only)."""
import re
from slopscan import rule, F, theme_file
from ktlite import split_top, named_arg, modifier_arg

CLICKABLE_HOLDERS = {"IconButton", "IconToggleButton", "FilledIconButton", "FilledTonalIconButton", "OutlinedIconButton", "FloatingActionButton",
                     "SmallFloatingActionButton", "LargeFloatingActionButton"}

def chains(k):
    """(start, end) of every 'Modifier.a(..).b{..}' chain."""
    out = []
    for m in re.finditer(r"(?<![\w.])Modifier\b", k.skel):
        j = m.end()
        while True:
            q = j
            while q < len(k.skel) and k.skel[q] in " \t\r\n": q += 1
            if q < len(k.skel) and k.skel[q] == "." and re.match(r"[A-Za-z_]", k.skel[q + 1:q + 2]):
                r = re.match(r"\.\s*\w+", k.skel[q:]); j = q + r.end()
                while j < len(k.skel) and k.skel[j] in " \t": j += 1
                if j < len(k.skel) and k.skel[j] in "({" and j in k.match: j = k.match[j] + 1
            else: break
        out.append((m.start(), j))
    return out

def content_desc(k, c):
    cd = named_arg(k.args(c), "contentDescription")
    if cd is None:
        parts = split_top(k.args(c))
        if len(parts) >= 2 and not re.match(r"\s*\w+\s*=", parts[1]): cd = parts[1].strip()
    return cd

@rule("U01")
def u01(p):
    """Icon/Image with contentDescription null (or absent) acting as the only content of a clickable control (IconButton/FAB/toggle) or carrying its own click modifier."""
    out = []
    for k in p.kt:
        for c in k.calls(r"Icon|Image"):
            cd = content_desc(k, c)
            if cd is not None and cd.strip() != "null": continue
            own = modifier_arg(k.args(c)) or ""
            holder = None
            for b in k.enclosing(c.start):
                lab = b[2].lstrip("=")
                if lab in CLICKABLE_HOLDERS and not re.search(r"\bText\s*\(", k.code[b[0]:b[1]]): holder = lab
            if holder or re.search(r"\.(clickable|combinedClickable|toggleable)\b", own):
                out.append(F("U01", k.path, k.line(c.start), f"{c.name} contentDescription={cd or 'absent'} is the only content of {holder or 'a clickable modifier'}"))
    return out

@rule("U02")
def u02(p):
    """Modifier chain with a clickable/toggleable/selectable and an explicit size/width/height under 48dp, without minimumInteractiveComponentSize or a 48dp minimum."""
    out = []
    for k in p.kt:
        for a, b in chains(k):
            ch = k.skel[a:b]
            if not re.search(r"\.(clickable|combinedClickable|toggleable|selectable)\b", ch): continue
            if re.search(r"minimumInteractiveComponentSize|sizeIn\s*\([^)]*4[89]|defaultMinSize|requiredSizeIn", ch): continue
            small = [m.group(0) for m in re.finditer(r"\.(size|width|height)\s*\(\s*(\d+(?:\.\d+)?)\.dp", ch) if float(m.group(2)) < 48]
            if small: out.append(F("U02", k.path, k.line(a), f"clickable chain with {small[0]} (< 48dp touch target)"))
    return out

NAMED = {"White": "FFFFFF", "Black": "000000", "Gray": "888888", "LightGray": "CCCCCC", "DarkGray": "444444", "Red": "FF0000", "Green": "00FF00",
         "Blue": "0000FF", "Yellow": "FFFF00", "Cyan": "00FFFF", "Magenta": "FF00FF"}
def lum(h):
    r, g, b = (int(h[i:i + 2], 16) / 255 for i in (0, 2, 4))
    f = lambda c: c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4
    return 0.2126 * f(r) + 0.7152 * f(g) + 0.0722 * f(b)
def ratio(a, b):
    la, lb = sorted((lum(a), lum(b)), reverse=True); return (la + 0.05) / (lb + 0.05)

@rule("U03")
def u03(p):
    """Material colour-scheme foreground/background pairs (on* vs base, both explicit) with WCAG contrast below 4.5:1; Text colour literal Gray/LightGray on the default light surface."""
    vals = {}
    for k in p.kt:
        for m in re.finditer(r"\bval\s+(\w+)\s*=\s*Color\(\s*0x([0-9A-Fa-f]{6,8})\s*\)", k.code): vals[m.group(1)] = m.group(2)[-6:].upper()
    def resolve(expr):
        if expr is None: return None
        e = expr.strip().rstrip(",")
        m = re.fullmatch(r"Color\(\s*0x([0-9A-Fa-f]{6,8})\s*\)", e)
        if m: return m.group(1)[-6:].upper()
        m = re.fullmatch(r"Color\.(\w+)", e)
        if m: return NAMED.get(m.group(1))
        return vals.get(e)
    out = []
    pairs = [("primary", "onPrimary"), ("secondary", "onSecondary"), ("tertiary", "onTertiary"), ("background", "onBackground"), ("surface", "onSurface"),
             ("error", "onError"), ("primaryContainer", "onPrimaryContainer"), ("secondaryContainer", "onSecondaryContainer"), ("surfaceVariant", "onSurfaceVariant")]
    for k in p.kt:
        for c in k.calls(r"lightColorScheme|darkColorScheme"):
            for base, on in pairs:
                a, b = resolve(named_arg(k.args(c), base)), resolve(named_arg(k.args(c), on))
                if a and b and ratio(a, b) < 4.5:
                    out.append(F("U03", k.path, k.line(c.start), f"{c.name}: {on} on {base} contrast {ratio(a, b):.2f}:1 < 4.5"))
        if not theme_file(k.path):
            for m in re.finditer(r"\bcolor\s*=\s*(Color\.(?:Gray|LightGray)|Color\(\s*0x[0-9A-Fa-f]{6,8}\s*\))", k.code):
                h = resolve(m.group(1))
                if h and ratio(h, "FFFBFE") < 4.5 and re.search(r"Text\s*\(", k.code[max(0, m.start() - 200):m.start()]):
                    out.append(F("U03", k.path, k.line(m.start()), f"Text colour {m.group(1)} on the default light surface {ratio(h, 'FFFBFE'):.2f}:1 < 4.5"))
    return out

@rule("U04")
def u04(p):
    """Colour literal (Color(0x..), Color.<Named> except Transparent/Unspecified) or fontSize = N.sp literal outside the theme package and previews."""
    out = []
    for k in p.kt:
        if theme_file(k.path): continue
        prev = [(f.start, f.bend) for f in k.funs if "@Preview" in k.code[max(0, f.start - 200):f.start]]
        for m in re.finditer(r"\bColor\(\s*0x[0-9A-Fa-f]{6,8}\s*\)|\bColor\.(?!Transparent|Unspecified)(White|Black|Gray|LightGray|DarkGray|Red|Green|Blue|Yellow|Cyan|Magenta)\b|\bfontSize\s*=\s*\d+(\.\d+)?\.sp", k.skel):
            if any(a <= m.start() <= b for a, b in prev): continue
            out.append(F("U04", k.path, k.line(m.start()), f"hardcoded {k.snippet(m.start(), n=40)} bypasses the theme"))
    return out

@rule("U05")
def u05(p):
    """Composable file that collects ViewModel/async state but renders no loading indicator, no error/retry UI, or no empty-state branch (one finding per missing state per file)."""
    out = []
    LOAD = re.compile(r"CircularProgressIndicator|LinearProgressIndicator|LoadingIndicator|\bisLoading\b|\bLoading\b|shimmer|\.placeholder\(|\bbusy\b|inProgress|submitting|Spinner|signing in", re.I)
    ERR = re.compile(r"\bError\b|\bisError\b|\berror\w*\b|retry|Snackbar|failure|\.message|errorMessage", re.I)
    EMPTY = re.compile(r"isEmpty\(\)|isNotEmpty\(\)|isNullOrEmpty|No\s+\w+(\s+\w+)?\s+(found|yet)|\bEmpty\b|nothing here", re.I)
    for k in p.kt:
        if theme_file(k.path) or not any(f.composable for f in k.funs): continue
        if not re.search(r"collectAsState|collectAsStateWithLifecycle|produceState|observeAsState|\bviewModel\s*\(|hiltViewModel|LazyPagingItems", k.skel): continue
        if not re.search(r"Screen|Page|Route", " ".join(f.name for f in k.funs if f.composable)): continue
        lists = bool(re.search(r"Lazy(Column|Row|\w*Grid)|\.forEach|\bitems\s*\(", k.skel))
        for kind, rx in (("loading", LOAD), ("error", ERR), ("empty", EMPTY)):
            if kind == "empty" and not lists: continue     # an empty state only exists where a list is shown
            if not rx.search(k.code):
                out.append(F("U05", k.path, 1, f"data screen file renders no {kind} state"))
    return out
