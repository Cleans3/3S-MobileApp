#!/usr/bin/env python3
"""scale_probe.py — Stage-1 static scale-readiness gate & automated Kotlin scale amplifier.

Addresses the "Toy-Data Illusion" observed in E6 (11/12 arm-O apps hardcode 8-12 Book literals
in memory with no DB/network/generator, passing naive memory checks) and rare-but-critical
E2 scale failures (P06 image decode, P09 eager list, P13 unindexed search, P08 item effects).

Commands:
  scale_probe.py audit <project_dir> [--json] [--strict]
  scale_probe.py amplify <project_dir> --out <out_dir> [--target-n 100000] [--emit-gate]
"""
from __future__ import annotations

import argparse
import json
import os
import pathlib
import re
import shutil
import sys
from dataclasses import asdict, dataclass, field
from typing import Dict, List, Optional, Tuple

SKIP_DIRS = {"build", ".gradle", ".git", ".idea", "node_modules", ".kotlin", "out", ".cxx"}
TEST_DIR_RE = re.compile(
    r"(^|/)(src/(test|androidTest|sharedTest|testFixtures|debug)|test|tests|androidTest|"
    r"benchmark|baselineprofile|macrobenchmark|sample|samples|screenshotTest)(/|$)"
)
LAZY_RE = re.compile(
    r"(?<![\w.])(LazyColumn|LazyRow|LazyVerticalGrid|LazyHorizontalGrid|"
    r"LazyVerticalStaggeredGrid|LazyHorizontalStaggeredGrid)\s*\("
)
STATIC_ITER_RE = re.compile(
    r"listOf\(|arrayOf\(|\.entries|values\(\)|\bindices\b|\d+\s*(?:until|\.\.)\s*\d+|"
    r"tabs|chips|subjects|authors|genres|filters|options|steps|fields|menu|destinations|"
    r"sections|labels|categories|stars|ratings",
    re.I,
)
DOMAIN_MODEL_RE = re.compile(
    r"^(?:Book|Title|LibraryBook|BookItem|Loan|LoanRecord|CatalogItem|CatalogBook|"
    r"Item|Product|Article|Post|Note|Task|Song|Movie|User|Member)\w*$"
)
EFFECT_PATTERNS = [
    ("blur", re.compile(r"\.blur\s*\(|\bRenderEffect\b|createBlurEffect")),
    ("gradient", re.compile(r"Brush\.(?:linear|vertical|horizontal|radial|sweep)Gradient")),
    ("graphicsLayer", re.compile(r"graphicsLayer\b[^\n;]{0,160}?\b(?:alpha|renderEffect|shadowElevation)\b")),
    ("infinite-anim", re.compile(r"rememberInfiniteTransition|infiniteRepeatable")),
]
ELEV_RE = re.compile(
    r"(?:\.shadow\s*\(\s*(?:elevation\s*=\s*)?|shadowElevation\s*=\s*|defaultElevation\s*=\s*|"
    r"\belevation\s*=\s*)(\d+(?:\.\d+)?)\.dp"
)
BOUNDED_MOD_RE = re.compile(
    r"\b(?:size|width|height|fillMax\w*|aspectRatio|weight|sizeIn|requiredSize|"
    r"requiredWidth|requiredHeight|defaultMinSize)\b"
)
INDEXED_SEARCH_RE = re.compile(
    r"@Fts[45]|USING\s+fts[45]|\bMATCH\s*[:'\"?]|invertedIndex|\bTrie\b|\btrie\b|"
    r"prefixIndex|searchIndex|Lucene|MiniSearch"
)
SCALABLE_STORE_RE = re.compile(
    r"@Database\b|Room\.databaseBuilder|SQLiteOpenHelper|Retrofit\.Builder|"
    r"PagingSource|Pager\s*\(|HttpClient\s*\("
)


def strip_comments_preserve_lines(src: str) -> str:
    """Replace // and /* */ comments with spaces (keeping newlines) while respecting string literals."""
    out = []
    i, n = 0, len(src)
    in_str = False
    triple = False
    while i < n:
        ch = src[i]
        if in_str:
            out.append(ch)
            if triple:
                if src[i : i + 3] == '"""':
                    out.append('""')
                    i += 3
                    in_str = False
                    triple = False
                    continue
            else:
                if ch == "\\" and i + 1 < n:
                    out.append(src[i + 1])
                    i += 2
                    continue
                if ch == '"':
                    in_str = False
            i += 1
            continue
        if src[i : i + 3] == '"""':
            in_str = True
            triple = True
            out.append('"""')
            i += 3
            continue
        if ch == '"':
            in_str = True
            out.append(ch)
            i += 1
            continue
        if src[i : i + 2] == "//":
            j = src.find("\n", i)
            if j == -1:
                out.append(" " * (n - i))
                break
            out.append(" " * (j - i))
            i = j
            continue
        if src[i : i + 2] == "/*":
            j = src.find("*/", i + 2)
            if j == -1:
                out.append(re.sub(r"[^\n]", " ", src[i:]))
                break
            out.append(re.sub(r"[^\n]", " ", src[i : j + 2]))
            i = j + 2
            continue
        out.append(ch)
        i += 1
    return "".join(out)


def find_matching(src: str, open_idx: int) -> int:
    """Return index of matching closing bracket for src[open_idx], respecting strings."""
    pairs = {"(": ")", "{": "}", "[": "]"}
    open_c = src[open_idx]
    close_c = pairs[open_c]
    depth = 0
    i, n = open_idx, len(src)
    in_str = False
    triple = False
    while i < n:
        ch = src[i]
        if in_str:
            if triple:
                if src[i : i + 3] == '"""':
                    in_str = False
                    triple = False
                    i += 3
                    continue
            else:
                if ch == "\\" and i + 1 < n:
                    i += 2
                    continue
                if ch == '"':
                    in_str = False
            i += 1
            continue
        if src[i : i + 3] == '"""':
            in_str = True
            triple = True
            i += 3
            continue
        if ch == '"':
            in_str = True
            i += 1
            continue
        if ch == open_c:
            depth += 1
        elif ch == close_c:
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return -1


def line_no(src: str, pos: int) -> int:
    return src.count("\n", 0, max(0, pos)) + 1


def split_top_level(text: str, sep: str = ",") -> List[str]:
    parts, buf = [], []
    dp = db = ds = 0
    in_str = False
    i, n = 0, len(text)
    while i < n:
        ch = text[i]
        if in_str:
            buf.append(ch)
            if ch == "\\" and i + 1 < n:
                buf.append(text[i + 1])
                i += 2
                continue
            if ch == '"':
                in_str = False
            i += 1
            continue
        if ch == '"':
            in_str = True
            buf.append(ch)
        elif ch == "(":
            dp += 1
            buf.append(ch)
        elif ch == ")":
            dp -= 1
            buf.append(ch)
        elif ch == "{":
            db += 1
            buf.append(ch)
        elif ch == "}":
            db -= 1
            buf.append(ch)
        elif ch == "[":
            ds += 1
            buf.append(ch)
        elif ch == "]":
            ds -= 1
            buf.append(ch)
        elif ch == sep and dp == 0 and db == 0 and ds == 0:
            parts.append("".join(buf).strip())
            buf = []
        else:
            buf.append(ch)
        i += 1
    tail = "".join(buf).strip()
    if tail:
        parts.append(tail)
    return parts


@dataclass
class DataClassInfo:
    name: str
    file: str
    fields: Dict[str, str] = field(default_factory=dict)


@dataclass
class ToyListMatch:
    file: str
    line: int
    model: str
    item_count: int
    start: int
    end: int
    var_name: str


@dataclass
class Hazard:
    code: str
    severity: str
    target_threatened: str
    file: str
    line: int
    evidence: str


def collect_kt_files(project_dir: pathlib.Path) -> List[Tuple[str, str, str]]:
    """Return list of (rel_path, raw_text, clean_text) for non-test Kotlin sources."""
    root = project_dir.resolve()
    files = []
    for dp, dn, fn in os.walk(root):
        dn[:] = sorted(d for d in dn if d not in SKIP_DIRS)
        for f in sorted(fn):
            if not f.endswith(".kt"):
                continue
            p = pathlib.Path(dp) / f
            rel = p.relative_to(root).as_posix()
            if TEST_DIR_RE.search(rel):
                continue
            try:
                if p.stat().st_size > 2_000_000:
                    continue
                raw = p.read_text(errors="replace")
            except OSError:
                continue
            files.append((rel, raw, strip_comments_preserve_lines(raw)))
    return files


def parse_data_classes(kt_files: List[Tuple[str, str, str]]) -> Dict[str, DataClassInfo]:
    classes: Dict[str, DataClassInfo] = {}
    for rel, _raw, clean in kt_files:
        for m in re.finditer(r"\bdata\s+class\s+(\w+)\s*(?:<[^>]*>)?\s*\(", clean):
            name = m.group(1)
            op = m.end() - 1
            cl = find_matching(clean, op)
            if cl == -1:
                continue
            params = split_top_level(clean[op + 1 : cl])
            fields: Dict[str, str] = {}
            for prm in params:
                pm = re.search(r"\b(?:val|var)\s+(\w+)\s*:\s*([^=]+?)(?:\s*=.*)?$", prm, re.S)
                if pm:
                    fields[pm.group(1)] = " ".join(pm.group(2).split())
            classes[name] = DataClassInfo(name=name, file=rel, fields=fields)
    return classes


def find_toy_lists(
    kt_files: List[Tuple[str, str, str]], data_classes: Dict[str, DataClassInfo]
) -> List[ToyListMatch]:
    """Find hardcoded listOf/mutableListOf collections holding 2..40 instances of a project data class."""
    known_models = set(data_classes.keys())
    matches: List[ToyListMatch] = []
    for rel, raw, clean in kt_files:
        for m in re.finditer(r"(?<![\w.])(?:listOf|mutableListOf)\s*(?:<[^>]+>)?\s*\(", clean):
            op = m.end() - 1
            cl = find_matching(clean, op)
            if cl == -1:
                continue
            body = clean[op + 1 : cl]
            elements = split_top_level(body)
            if not (2 <= len(elements) <= 40):
                continue
            # Identify constructor name of top-level elements
            ctors = []
            for el in elements:
                cm = re.match(r"^(?:[\w.]+\.)?([A-Z]\w*)\s*\(", el.strip())
                if cm:
                    ctors.append(cm.group(1))
            if not ctors or len(ctors) < max(2, int(0.8 * len(elements))):
                continue
            model = max(set(ctors), key=ctors.count)
            if model not in known_models and not DOMAIN_MODEL_RE.match(model):
                continue
            # Ignore small UI metadata classes (Tab, NavItem, Filter, Category, Chapter, Review)
            if re.search(r"(?:Tab|Nav|Filter|Chip|Menu|Category|Chapter|Review|Option|Action)$", model):
                continue
            prefix = clean[max(0, m.start() - 120) : m.start()]
            vm = re.search(r"\b(?:val|var)\s+(\w+)\s*(?::[^=]+)?=\s*$", prefix)
            var_name = vm.group(1) if vm else "<expr>"
            matches.append(
                ToyListMatch(
                    file=rel,
                    line=line_no(raw, m.start()),
                    model=model,
                    item_count=len(ctors),
                    start=m.start(),
                    end=cl + 1,
                    var_name=var_name,
                )
            )
    matches.sort(key=lambda x: (-x.item_count, x.file, x.line))
    return matches


def detect_large_generator(all_clean: str) -> int:
    """Return largest deterministic collection size N >= 500 detected in Kotlin sources, else 0."""
    nums: List[int] = []
    # Direct List(5000), List(5_000), repeat(5000), (1..5000), (0 until 5000)
    for m in re.finditer(
        r"(?:\bList|\bMutableList|\bArray|\brepeat)\s*\(\s*(\d[\d_]*)\b|"
        r"(?:\.\.|until)\s*(\d[\d_]*)\b",
        all_clean,
    ):
        raw_n = m.group(1) or m.group(2)
        val = int(raw_n.replace("_", ""))
        if val >= 500:
            nums.append(val)
    # Named val/const val or generator function parameter (never Book(reviewCount = 5400) named args)
    for m in re.finditer(
        r"\b(?:const\s+)?val\s+(?:CATALOG|BOOK|TITLE|ITEM|TOTAL|SEED|DEFAULT)[\w_]*\s*(?::\s*Int)?\s*=\s*(\d[\d_]*)\b|"
        r"\bfun\s+\w*(?:gen|build|create|seed|load)\w*\s*\([^)]*\b(?:count|size|n|total)\s*:\s*Int\s*=\s*(\d[\d_]*)\b",
        all_clean,
        re.I,
    ):
        raw_n = m.group(1) or m.group(2)
        val = int(raw_n.replace("_", ""))
        if val >= 500:
            nums.append(val)
    return max(nums) if nums else 0


def audit_project(project_dir: pathlib.Path) -> dict:
    kt_files = collect_kt_files(project_dir)
    if not kt_files:
        return {
            "project": str(project_dir),
            "kt_files": 0,
            "verdict": "INVALID",
            "reason": "no Kotlin source files found",
            "hazards": [],
        }

    data_classes = parse_data_classes(kt_files)
    toy_lists = find_toy_lists(kt_files, data_classes)
    all_clean = "\n".join(c for _, _, c in kt_files)

    max_gen_n = detect_large_generator(all_clean)
    has_store = bool(SCALABLE_STORE_RE.search(all_clean))
    has_scalable_source = has_store or (max_gen_n >= 500)
    toy_data_illusion = bool(toy_lists) and not has_scalable_source

    hazards: List[Hazard] = []

    if toy_data_illusion:
        top = toy_lists[0]
        hazards.append(
            Hazard(
                code="SCALE-TOY",
                severity="high",
                target_threatened="<150 MB RSS / <50 ms search (hidden by toy data)",
                file=top.file,
                line=top.line,
                evidence=f"{top.var_name} hardcodes {top.item_count} {top.model} literals; no DB, network, paging, or N>=500 generator",
            )
        )

    # 1. SCALE-SEARCH (P13-class): in-memory linear filter or unindexed SQL LIKE
    indexed = bool(INDEXED_SEARCH_RE.search(all_clean))
    debounced = bool(re.search(r"\bdebounce\s*\(", all_clean))
    if not indexed:
        for rel, raw, clean in kt_files:
            for m in re.finditer(r"\.(filter|filterNot)\s*\{", clean):
                op = m.end() - 1
                cl = find_matching(clean, op)
                if cl == -1:
                    continue
                body = clean[op : cl + 1]
                ctx = clean[max(0, m.start() - 180) : m.start()]
                if re.search(r"\b(?:contains|startsWith|indexOf|matches)\s*\(", body) and re.search(
                    r"\b(?:query|search|searchQuery|searchText|keyword|term|q)\b", body + ctx, re.I
                ):
                    hazards.append(
                        Hazard(
                            code="SCALE-SEARCH",
                            severity="critical",
                            target_threatened="<50 ms search over 100k items",
                            file=rel,
                            line=line_no(raw, m.start()),
                            evidence=f"linear .{m.group(1)} on search query without FTS/index (debounce={'yes' if debounced else 'no'})",
                        )
                    )
                    break
            for m in re.finditer(r"@Query\s*\(([^)]*\bLIKE\b[^)]*)\)", clean, re.S):
                hazards.append(
                    Hazard(
                        code="SCALE-SEARCH",
                        severity="critical",
                        target_threatened="<50 ms search over 100k items",
                        file=rel,
                        line=line_no(raw, m.start()),
                        evidence=f"unindexed SQL LIKE query without @Fts4/@Fts5 (debounce={'yes' if debounced else 'no'})",
                    )
                )

    # 2. SCALE-EAGER-LIST (P09-class): Column/Row + scroll + loop
    for rel, raw, clean in kt_files:
        for m in re.finditer(r"(?<![\w.])(Column|Row)\s*\(", clean):
            op = m.end() - 1
            cl = find_matching(clean, op)
            if cl == -1:
                continue
            args = clean[op + 1 : cl]
            if not re.search(r"verticalScroll|horizontalScroll", args):
                continue
            # Find trailing lambda
            j = cl + 1
            while j < len(clean) and clean[j].isspace():
                j += 1
            if j >= len(clean) or clean[j] != "{":
                continue
            lcl = find_matching(clean, j)
            if lcl == -1:
                continue
            body = clean[j : lcl + 1]
            for lm in re.finditer(r"\.forEach(?:Indexed)?\s*\{|\bfor\s*\([^)]+\bin\s+([^)]+)\)", body):
                ctx = body[max(0, lm.start() - 50) : lm.end() + 40]
                if STATIC_ITER_RE.search(ctx):
                    continue
                if re.search(r"(?<![\w.])[A-Z]\w*\s*[({]", body[lm.end() : lm.end() + 600]):
                    hazards.append(
                        Hazard(
                            code="SCALE-EAGER-LIST",
                            severity="critical",
                            target_threatened="<150 MB RSS & 60 fps",
                            file=rel,
                            line=line_no(raw, j + lm.start()),
                            evidence=f"{m.group(1)}+scroll iterates collection eagerly instead of Lazy layout",
                        )
                    )
                    break

    # 3. SCALE-IMAGE (P06-class): full-res decode or unbounded AsyncImage
    for rel, raw, clean in kt_files:
        if "BitmapFactory" in clean and "inSampleSize" not in clean:
            for m in re.finditer(r"BitmapFactory\.decode\w+", clean):
                hazards.append(
                    Hazard(
                        code="SCALE-IMAGE",
                        severity="critical",
                        target_threatened="<150 MB RSS",
                        file=rel,
                        line=line_no(raw, m.start()),
                        evidence=f"{m.group(0)} without inSampleSize",
                    )
                )
        for m in re.finditer(r"Size\.ORIGINAL|CachePolicy\.DISABLED|DiskCacheStrategy\.NONE|SIZE_ORIGINAL", clean):
            hazards.append(
                Hazard(
                    code="SCALE-IMAGE",
                    severity="critical",
                    target_threatened="<150 MB RSS",
                    file=rel,
                    line=line_no(raw, m.start()),
                    evidence=f"{m.group(0)} disables image downsampling/caching",
                )
            )
        for m in re.finditer(r"(?<![\w.])(AsyncImage|SubcomposeAsyncImage)\s*\(", clean):
            op = m.end() - 1
            cl = find_matching(clean, op)
            if cl == -1:
                continue
            args = clean[op + 1 : cl]
            if not BOUNDED_MOD_RE.search(args):
                hazards.append(
                    Hazard(
                        code="SCALE-IMAGE",
                        severity="critical",
                        target_threatened="<150 MB RSS",
                        file=rel,
                        line=line_no(raw, m.start()),
                        evidence=f"{m.group(1)} without size-bounding modifier",
                    )
                )

    # 4. SCALE-FX-ITEM (P08-class): heavy GPU/animation effects inside repeated lazy items()/itemsIndexed() scopes
    #    (excludes single header `item { ... }` blocks and a standalone 2-stop flat gradient tile without other effects)
    composable_bodies: Dict[str, List[Tuple[str, str, str, int]]] = {}
    for rel, raw, clean in kt_files:
        for fm in re.finditer(r"@Composable[\s\S]{0,120}?\bfun\s+([A-Z]\w*)\s*\(", clean):
            op = fm.end() - 1
            cl = find_matching(clean, op)
            if cl == -1:
                continue
            j = cl + 1
            while j < len(clean) and clean[j] not in "{=":
                j += 1
            if j < len(clean) and clean[j] == "{":
                bcl = find_matching(clean, j)
                if bcl != -1:
                    composable_bodies.setdefault(fm.group(1), []).append((rel, raw, clean[j : bcl + 1], j))

    fx_hits = set()
    for rel, raw, clean in kt_files:
        for lm in LAZY_RE.finditer(clean):
            op = lm.end() - 1
            cl = find_matching(clean, op)
            if cl == -1:
                continue
            j = cl + 1
            while j < len(clean) and clean[j].isspace():
                j += 1
            if j >= len(clean) or clean[j] != "{":
                continue
            lcl = find_matching(clean, j)
            if lcl == -1:
                continue
            lazy_body = clean[j : lcl + 1]
            # Extract only repeated `items(...) { ... }` and `itemsIndexed(...) { ... }` lambdas
            queue = []
            for im in re.finditer(r"(?<![\w.])(items|itemsIndexed)\s*\(", lazy_body):
                iop = im.end() - 1
                icl = find_matching(lazy_body, iop)
                if icl == -1:
                    continue
                k = icl + 1
                while k < len(lazy_body) and lazy_body[k].isspace():
                    k += 1
                if k < len(lazy_body) and lazy_body[k] == "{":
                    ilcl = find_matching(lazy_body, k)
                    if ilcl != -1:
                        queue.append((rel, raw, lazy_body[k : ilcl + 1], j + k, 0))
            seen_scopes = set()
            scope_effects = []
            while queue:
                srel, sraw, sbody, soff, depth = queue.pop(0)
                key = (srel, soff)
                if key in seen_scopes:
                    continue
                seen_scopes.add(key)
                for ename, rx in EFFECT_PATTERNS:
                    em = rx.search(sbody)
                    if em:
                        scope_effects.append((ename, srel, line_no(sraw, soff + em.start()), em.group(0)[:45]))
                for em in ELEV_RE.finditer(sbody):
                    if float(em.group(1)) > 2.0:
                        scope_effects.append(("shadow>2dp", srel, line_no(sraw, soff + em.start()), f"{em.group(1)}dp"))
                if depth < 3:
                    for cm in re.finditer(r"(?<![\w.])([A-Z]\w*)\s*\(", sbody):
                        for child in composable_bodies.get(cm.group(1), []):
                            queue.append((*child, depth + 1))

            heavy_names = {"blur", "graphicsLayer", "infinite-anim", "shadow>2dp"}
            has_heavy = any(e[0] in heavy_names for e in scope_effects)
            distinct_types = {e[0] for e in scope_effects}
            for ename, srel, sln, snip in scope_effects:
                # Flag any heavy GPU/animation effect, or gradient only when stacked with another effect
                if ename in heavy_names or (ename == "gradient" and (has_heavy or len(distinct_types) >= 2)):
                    if (srel, ename) not in fx_hits:
                        fx_hits.add((srel, ename))
                        hazards.append(
                            Hazard(
                                code="SCALE-FX-ITEM",
                                severity="high",
                                target_threatened="60 fps (frame p90 +28% in E2 C5)",
                                file=srel,
                                line=sln,
                                evidence=f"{ename} ({snip}) in repeated lazy items() scope",
                            )
                        )

    codes = sorted({h.code for h in hazards})
    verdict = "FAIL" if hazards else "PASS"
    return {
        "project": str(project_dir),
        "kt_files": len(kt_files),
        "toy_list_count": toy_lists[0].item_count if toy_lists else 0,
        "toy_model": toy_lists[0].model if toy_lists else "",
        "max_generator_n": max_gen_n,
        "has_scalable_store": has_store,
        "toy_data_illusion": toy_data_illusion,
        "hazard_codes": codes,
        "hazard_count": len(hazards),
        "verdict": verdict,
        "hazards": [asdict(h) for h in hazards],
    }


def _copy_expr_for_model(info: Optional[DataClassInfo]) -> str:
    """Build deterministic .copy(...) arguments that preserve unique keys and distinct titles."""
    if info is None or not info.fields:
        return "_b"
    assigns = []
    if "id" in info.fields:
        t = info.fields["id"].rstrip("?").split(".")[-1]
        if t == "Int":
            assigns.append("id = _idx + 1")
        elif t == "Long":
            assigns.append("id = (_idx + 1).toLong()")
        elif t == "String":
            assigns.append('id = "${_b.id}-${_idx + 1}"')
    if "title" in info.fields and "String" in info.fields["title"]:
        assigns.append('title = "${_b.title} #${_idx + 1}"')
    elif "name" in info.fields and "String" in info.fields["name"]:
        assigns.append('name = "${_b.name} #${_idx + 1}"')
    if "isbn" in info.fields and "String" in info.fields["isbn"]:
        assigns.append('isbn = "978-${100000000 + _idx}"')
    if not assigns:
        return "_b"
    return f"_b.copy({', '.join(assigns)})"


def amplify_project(
    project_dir: pathlib.Path, out_dir: pathlib.Path, target_n: int = 100_000, emit_gate: bool = True
) -> dict:
    """Copy project_dir to out_dir and rewrite its primary toy listOf(...) to a deterministic List(target_n)."""
    src_root = project_dir.resolve()
    dst_root = out_dir.resolve()
    if dst_root.exists():
        shutil.rmtree(dst_root)
    shutil.copytree(
        src_root,
        dst_root,
        ignore=shutil.ignore_patterns("build", ".gradle", ".git", ".kotlin", ".cxx"),
    )

    kt_files = collect_kt_files(dst_root)
    data_classes = parse_data_classes(kt_files)
    toy_lists = find_toy_lists(kt_files, data_classes)
    if not toy_lists:
        return {
            "status": "SKIPPED",
            "reason": "no hardcoded toy listOf(...) of 2..40 domain items found",
            "out_dir": str(dst_root),
        }

    target = toy_lists[0]
    target_path = dst_root / target.file
    raw = target_path.read_text(errors="replace")
    orig_expr = raw[target.start : target.end]
    info = data_classes.get(target.model)
    copy_expr = _copy_expr_for_model(info)

    replacement = (
        "run {\n"
        f"    val _scaleBase = {orig_expr}\n"
        f"    List({target_n}) {{ _idx ->\n"
        "        val _b = _scaleBase[_idx % _scaleBase.size]\n"
        f"        {copy_expr}\n"
        "    }\n"
        "}"
    )
    rewritten = raw[: target.start] + replacement + raw[target.end :]

    # Structural sanity check: braces and parentheses remain balanced on comment-stripped code
    clean_new = strip_comments_preserve_lines(rewritten)
    balanced = (
        clean_new.count("{") == clean_new.count("}")
        and clean_new.count("(") == clean_new.count(")")
        and clean_new.count("[") == clean_new.count("]")
    )
    if not balanced:
        raise RuntimeError(f"Amplifier produced unbalanced brackets in {target.file}")

    target_path.write_text(rewritten)

    manifest = {
        "status": "AMPLIFIED",
        "source_project": str(src_root),
        "out_dir": str(dst_root),
        "modified_file": target.file,
        "line": target.line,
        "var_name": target.var_name,
        "model": target.model,
        "original_n": target.item_count,
        "target_n": target_n,
        "amplification_factor": round(target_n / max(1, target.item_count), 1),
        "copy_expr": copy_expr,
        "brackets_balanced": balanced,
    }
    (dst_root / "scale_probe_manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")

    if emit_gate:
        gate_src = pathlib.Path(__file__).resolve().parent / "ScaleBudgetGate.kt"
        if gate_src.exists():
            gate_dst_dir = dst_root / "scale-probe-gate"
            gate_dst_dir.mkdir(exist_ok=True)
            shutil.copy2(gate_src, gate_dst_dir / "ScaleBudgetGate.kt")
            manifest["gate_template"] = "scale-probe-gate/ScaleBudgetGate.kt"

    return manifest


def main() -> int:
    ap = argparse.ArgumentParser(description="Scale Probe: audit & amplify toy-data Android projects")
    sub = ap.add_subparsers(dest="cmd", required=True)

    pa = sub.add_parser("audit", help="Audit an Android project for toy-data illusion and scale hazards")
    pa.add_argument("project_dir", type=pathlib.Path)
    pa.add_argument("--json", action="store_true")
    pa.add_argument("--strict", action="store_true", help="Exit 1 on FAIL verdict")

    pm = sub.add_parser("amplify", help="Create a scale-amplified copy of a toy-data Android project")
    pm.add_argument("project_dir", type=pathlib.Path)
    pm.add_argument("--out", type=pathlib.Path, required=True)
    pm.add_argument("--target-n", type=int, default=100_000)
    pm.add_argument("--no-gate", action="store_true")

    args = ap.parse_args()
    if args.cmd == "audit":
        res = audit_project(args.project_dir)
        if args.json:
            print(json.dumps(res, indent=2))
        else:
            print(
                f"{res['verdict']:7s} | toy={res.get('toy_list_count',0)} ({res.get('toy_model','') or '-'}) "
                f"| gen_n={res.get('max_generator_n',0)} | store={res.get('has_scalable_store',False)} "
                f"| hazards={res.get('hazard_codes',[])}"
            )
            for h in res.get("hazards", []):
                print(f"  [{h['code']}] {h['file']}:{h['line']} — {h['evidence']} (target: {h['target_threatened']})")
        return 1 if (args.strict and res["verdict"] != "PASS") else 0

    if args.cmd == "amplify":
        res = amplify_project(args.project_dir, args.out, target_n=args.target_n, emit_gate=not args.no_gate)
        print(json.dumps(res, indent=2))
        return 0 if res["status"] == "AMPLIFIED" else 2

    return 0


if __name__ == "__main__":
    sys.exit(main())
