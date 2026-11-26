#!/usr/bin/env python3
"""Check that every number shown in presentation.html is proven in deck-proof.csv.

Usage: python3 check_deck_proof.py [--slide N] [--list N] [--proof FILE]
  --list N   print the data tokens of slide N (what needs a proof row) and exit
Exit 0 only when every row checks out and every token of the checked slides is covered.
Stdlib only. Run from any directory.
"""
import csv, html, math, re, sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
STUDY = HERE.parent
REPO = STUDY.parent.parent
DECK = STUDY / "presentation.html"
PROOF = HERE / "deck-proof.csv"

VN_ATTRS = ("data-n", "data-k", "data-count", "data-vals", "data-from", "data-tp")
JS_ATTRS = ("data-est", "data-lo", "data-hi", "data-thr")
STYLE_VARS = ("--v", "--t", "--lo", "--hi")
# a number not glued to a letter (so E1, C5, P08, R8 are IDs, not data)
NUM = re.compile(r"(?<![\w.,])[−+-]?\d+(?:[.,]\d+)*(?:\s?%)?(?![\w])", re.UNICODE)


def norm(tok, js=False):
    """Normalised value string of a displayed number, e.g. '1.901' -> '1901', '90,05 %' -> '90.05'."""
    t = tok.replace("−", "-").replace(" ", "").replace("%", "").lstrip("+")
    if js:
        s = t
    elif "," in t:
        s = t.replace(".", "").replace(",", ".")
    elif re.fullmatch(r"-?\d{1,3}(\.\d{3})+", t):
        s = t.replace(".", "")
    else:
        s = t
    try:
        f = float(s)
    except ValueError:
        return t
    s = ("%.12f" % f).rstrip("0").rstrip(".")
    return "0" if s in ("", "-0") else s


def decimals(v):
    return len(v.split(".")[1]) if "." in v else 0


def slides():
    text = DECK.read_text(encoding="utf-8")
    stage = text[text.index('<main id="stage"'):]
    out = []
    for m in re.finditer(r'<section class="slide[^"]*"[^>]*data-title="([^"]*)"[^>]*>([\s\S]*?)</section>', stage):
        body = m.group(2)
        toks = []
        for attr, js in [(a, False) for a in VN_ATTRS] + [(a, True) for a in JS_ATTRS]:
            for v in re.findall(r"\s" + attr + r'="([^"]*)"', body):
                for part in re.split(r"[;]", v):
                    part = part.split(":", 1)[-1] if attr == "data-vals" else part
                    if part.strip():
                        toks.append((norm(part, js), part, attr))
        for style in re.findall(r'style="([^"]*)"', body):
            for name, val in re.findall(r"(--[a-z]+):\s*([-.\d]+)(?=;|$)", style):
                if name in STYLE_VARS:
                    toks.append((norm(val, True), val, "style" + name))
        clean = re.sub(r"<script[\s\S]*?</script>", " ", body)
        clean = html.unescape(re.sub(r"<[^>]+>", " ", clean))
        for t in NUM.findall(clean):
            toks.append((norm(t), t.strip(), "text"))
        out.append((m.group(1), toks))
    return out


def numbers_in(s):
    vals = set()
    for t in re.findall(r"[−+-]?\d+(?:[.,]\d+)*", s):
        t = t.replace("−", "-").lstrip("+")
        for cand in {t.replace(",", ""), t.replace(".", "").replace(",", "."), t.replace(",", ".")}:
            try:
                vals.add(float(cand))
            except ValueError:
                pass
    return vals


def matches(value, cands):
    v = float(value)
    d = decimals(value)
    for c in cands:
        for scale in (1, 100, 0.01, 1000, 0.001):
            for sign in (1, -1):
                if math.isclose(round(sign * c * scale, d), v, abs_tol=10 ** -(d + 6)):
                    return True
    return False


SAFE = re.compile(r"^[\d\s.+\-*/()]+$")


def check_row(r):
    kind, value = r["kind"], r["value"]
    if kind == "label":
        return None if r["note"].strip() else "label needs a note saying what the number is"
    if kind == "derived":
        f = r["formula"].strip()
        if not f or not SAFE.match(f):
            return "derived needs a formula of digits and + - * / ( )"
        got = eval(f, {"__builtins__": {}})  # noqa: S307 (SAFE-checked)
        d = decimals(value)
        if not math.isclose(round(got, d), float(value), abs_tol=10 ** -d / 2 + 1e-9) and not math.isclose(round(abs(got), d), abs(float(value)), abs_tol=10 ** -d / 2 + 1e-9):
            return f"formula gives {got!r}, shown {value}"
        if not r["source"]:
            return None
    if kind not in ("direct", "cited", "derived"):
        return f"unknown kind {kind!r}"
    src = (REPO / r["source"]) if r["source"] else None
    if not src or not src.is_file():
        return f"source not found: {r['source']!r}"
    try:
        line = src.read_text(encoding="utf-8").splitlines()[int(r["line"]) - 1]
    except (ValueError, IndexError):
        return f"line {r['line']!r} not in {r['source']}"
    if r["quote"] not in line:
        return f"quote not on {r['source']}:{r['line']}"
    if kind != "derived" and not matches(value, numbers_in(r["quote"])):
        return f"quote has no number matching {value}"
    return None


def main(argv):
    deck = slides()
    if "--list" in argv:
        n = int(argv[argv.index("--list") + 1])
        title, toks = deck[n - 1]
        print(f"slide {n} {title}")
        seen = {}
        for v, raw, where in toks:
            seen.setdefault(v, []).append(f"{raw} [{where}]")
        for v, raws in seen.items():
            print(f"  {v}\t{' | '.join(raws)}")
        return 0
    only = int(argv[argv.index("--slide") + 1]) if "--slide" in argv else None
    proof = Path(argv[argv.index("--proof") + 1]) if "--proof" in argv else PROOF
    rows = list(csv.DictReader(proof.open(encoding="utf-8"))) if proof.is_file() else []
    errors, covered = 0, {}
    for i, r in enumerate(rows, start=2):
        if only and int(r["slide"]) != only:
            continue
        r["value"] = norm(r["value"], True)
        err = check_row(r)
        if err:
            errors += 1
            print(f"ROW {i} slide {r['slide']} value {r['value']}: {err}")
        else:
            covered.setdefault(int(r["slide"]), set()).add(r["value"])
    stale = 0
    for i, r in enumerate(rows, start=2):
        n = int(r["slide"])
        if (only and n != only) or n > len(deck):
            continue
        if r["value"] not in {v for v, _, _ in deck[n - 1][1]}:
            stale += 1
            print(f"ROW {i} slide {n} value {r['value']}: stale, the number is no longer on the slide")
    missing = 0
    for n, (title, toks) in enumerate(deck, start=1):
        if only and n != only:
            continue
        need = {v for v, _, _ in toks}
        gap = sorted(need - covered.get(n, set()), key=lambda x: (len(x), x))
        if gap:
            missing += len(gap)
            print(f"SLIDE {n} {title}: {len(gap)} unproven: {', '.join(gap)}")
    total = sum(len({v for v, _, _ in t}) for k, (_, t) in enumerate(deck, 1) if not only or k == only)
    print(f"rows checked: {sum(1 for r in rows if not only or int(r['slide']) == only)}, bad rows: {errors}, stale rows: {stale}, data values: {total}, unproven: {missing}")
    return 1 if errors or missing or stale else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
