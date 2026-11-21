#!/usr/bin/env python3
"""Open-coding aid (protocol 5/R-open): seeded stratified pick of >=10 runs, one digest per run.

The digest is an index for reading, not a result: the coder reads the sources and writes
results/open_coding/codes.csv (run_id, code, evidence, in_taxonomy) for patterns the frozen
taxonomy does NOT name. Counters below are descriptive aids only and feed no endpoint.

  python3 harness/open_coding.py [--n 12] [--seed 20261121]
"""
import argparse, collections, csv, json, pathlib, random, re

E1 = pathlib.Path(__file__).resolve().parents[1]
RES, RUNS = E1 / "results", E1 / "runs"
AIDS = {
    "todo/fixme/stub": r"\b(TODO|FIXME|XXX|not implemented|stub)\b",
    "empty catch": r"catch\s*\([^)]*\)\s*\{\s*\}",
    "!! operator": r"!!",
    "GlobalScope/runBlocking": r"\b(GlobalScope|runBlocking)\b",
    "log/println": r"\b(println|Log\.[dviwe]|Timber\.)\(",
    "hardcoded mock data": r"\b(listOf|mutableListOf)\(\s*(Book|Title|Member|Loan)\w*\(",
    "http:// literal": r"\"http://",
    "commented-out code": r"^\s*//\s*(val|var|fun|if|for|return|import)\b",
    "@Suppress": r"@Suppress\(",
}
MANIFEST_RX = (r'uses-permission android:name="([^"]+)"',
               r'android:((?:exported|allowBackup|usesCleartextTraffic|debuggable)="\w+")')


def pick(plan, runs, n, seed, prefix="", skip=frozenset()):
    """Round-robin over (bundle, style) strata so every stratum is covered before any repeats. The order comes from the PLAN cells
    (seeded shuffle), not from which runs exist, so the sample does not depend on generation timing or outcomes. A planned run that
    exists but is invalid is skipped; one not generated yet is returned as pending."""
    strata = collections.defaultdict(list)
    for bundle, brief, style, rep in plan:
        strata[(bundle, style)].append(f"{prefix}{bundle}-{brief}-{style}-r{rep}")
    rng = random.Random(seed)
    for v in strata.values():
        rng.shuffle(v)
    keys = sorted(strata)
    rng.shuffle(keys)
    out = []
    while len(out) < n and any(strata.values()):
        for k in keys:
            while strata[k]:
                rid = strata[k].pop(0)
                if rid in skip or (rid in runs and runs[rid]["valid"] != "True"):
                    continue
                out.append(rid); break
            if len(out) >= n: break
    return out


def digest(r, findings):
    rid, src = r["run_id"], RUNS / r["run_id"] / "src"
    kt = sorted(src.rglob("*.kt"))
    lines = [f"# {rid}", f"bundle={r['bundle']} style={r['prompt_style']} brief={r['brief']} rep={r['rep']} "
             f"files={r['files']} kotlin_loc={r['kotlin_loc']} build={r['build']}", "", "## files"]
    for p in sorted(src.rglob("*")):
        if p.is_file():
            lines.append(f"- {p.relative_to(src)} ({len(p.read_text(errors='replace').splitlines())})")
    man = next(iter(src.rglob("AndroidManifest.xml")), None)
    if man:
        t = man.read_text(errors="replace")
        flags = sorted({m for rx in MANIFEST_RX for m in re.findall(rx, t)})
        lines += ["", "## manifest flags"] + [f"- {m}" for m in flags]
    lines += ["", "## descriptive counters (aid only)"]
    for name, rx in AIDS.items():
        n = sum(len(re.findall(rx, p.read_text(errors="replace"), re.M)) for p in kt)
        lines.append(f"- {name}: {n}")
    by = collections.Counter(f["rule_id"] for f in findings)
    lines += ["", "## detector findings (rule: n)"] + [f"- {k}: {v}" for k, v in sorted(by.items())]
    lines += ["", "## finding sites (first 3 per rule)"]
    seen = collections.Counter()
    for f in findings:
        seen[f["rule_id"]] += 1
        if seen[f["rule_id"]] <= 3:
            lines.append(f"- {f['rule_id']} {f['file']}:{f['line']} {f['evidence'][:110]}")
    lines += ["", "## coder notes (fill in; copy codes to codes.csv)", ""]
    return "\n".join(lines)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--n", type=int, default=12)
    ap.add_argument("--seed", type=int, default=20261121)
    ap.add_argument("--pilot", action="store_true", help="draw from pilot runs (tool check only)")
    ap.add_argument("--redraw", action="store_true", help="draw the sample again instead of keeping results/open_coding/sample.json")
    a = ap.parse_args()
    runs = {r["run_id"]: r for r in csv.DictReader(open(RES / "runs.csv")) if (r["pilot"] == "True") == a.pilot}
    plan = [tuple(c) for c in json.load(open(RES / "plan.json"))["cells"]] if not a.pilot else \
        [(r["bundle"], r["brief"], r["prompt_style"], r["rep"]) for r in runs.values()]
    ff = RES / ("pilot_findings.csv" if a.pilot else "findings.csv")
    fnd = collections.defaultdict(list)
    for f in csv.DictReader(open(ff)):
        fnd[f["run_id"]].append(f)
    out = RES / ("open_coding_pilot" if a.pilot else "open_coding")
    out.mkdir(exist_ok=True)
    # cells lost to infrastructure (infra_failures.csv, e.g. agy OAuth expiry) are skipped, so the next round-robin run takes their place (alpha, A11 draft)
    lost = {r["run_id"] for r in csv.DictReader(open(RES / "infra_failures.csv"))} - set(runs) if (RES / "infra_failures.csv").exists() and not a.pilot else set()
    if (out / "sample.json").exists() and not a.redraw:
        chosen = json.loads((out / "sample.json").read_text())      # the drawn sample is frozen once coding started
    else:
        chosen = pick(plan, runs, a.n, a.seed, "pilot-" if a.pilot else "", skip=lost)
        (out / "sample.json").write_text(json.dumps(chosen, indent=1))
    pending = [rid for rid in chosen if rid not in runs]
    for rid in chosen:
        if rid in runs:
            (out / f"{rid}.md").write_text(digest(runs[rid], fnd[rid]))
    if pending: print("pending (not generated yet):", pending)
    chosen = [rid for rid in chosen if rid in runs]
    if not a.pilot and not (out / "codes.csv").exists():
        (out / "codes.csv").write_text("run_id,code,evidence,in_taxonomy\n")
    print(f"{len(chosen)} digests -> {out}")


if __name__ == "__main__":
    main()
