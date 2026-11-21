#!/usr/bin/env python3
"""slop-scan: static detector for the frozen taxonomy IDs (contract 3) in an Android/Compose project.

  slopscan.py scan <project_dir> --id <run_id> [--out findings.json] [--csv findings.csv]

Source-level, stdlib only. Rule modules: r_perf (P*), r_sec (S*), r_ui (U*), r_stale (D01). Dependency checks (S11, D02) are
in depcheck.py (needs the network). Every rule is a function(project) -> [Finding]; its docstring is the rule's definition.
Findings carry detector=slop-scan; triage is filled later by hand ('unk' until then). Cited-only IDs (P12, S10, S12, U06) are NOT scanned (R14).
"""
import argparse, csv, json, os, pathlib, re, sys

HERE = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
if __name__ == "__main__": sys.modules["slopscan"] = sys.modules["__main__"]   # rule modules import 'slopscan': share one RULES list
from ktlite import Kt  # noqa: E402

SKIP_DIRS = {"build", ".gradle", ".git", ".idea", "node_modules", ".kotlin", "out", ".cxx"}
TEST_DIR = re.compile(r"(^|/)(src/(test|androidTest|sharedTest|testFixtures|debug)|test|tests|androidTest|benchmark|baselineprofile|macrobenchmark|sample|samples|screenshotTest)(/|$)")

class Project:
    def __init__(self, root):
        self.root = pathlib.Path(root).resolve()
        self.kt, self.xml, self.gradle, self.toml, self.other = [], [], [], [], []
        for dp, dn, fn in os.walk(self.root):
            dn[:] = [d for d in dn if d not in SKIP_DIRS]
            for f in fn:
                p = pathlib.Path(dp) / f
                rel = p.relative_to(self.root).as_posix()
                try:
                    if p.stat().st_size > 2_000_000: continue
                except OSError: continue
                if f.endswith(".kt") and not TEST_DIR.search(rel):
                    self.kt.append(Kt(rel, p.read_text(errors="replace")))
                elif f.endswith(".xml") and not TEST_DIR.search(rel):
                    self.xml.append((rel, p.read_text(errors="replace")))
                elif f in ("build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts"):
                    self.gradle.append((rel, p.read_text(errors="replace")))
                elif f.endswith(".toml") and "gradle" in rel:
                    self.toml.append((rel, p.read_text(errors="replace")))
                elif f in ("baseline-prof.txt", "proguard-rules.pro", "stability_config.conf") or f.endswith((".pro", ".properties")):
                    self.other.append((rel, p.read_text(errors="replace")))
        self.kt.sort(key=lambda k: k.path); self.xml.sort(); self.gradle.sort()
        self.funs = {}                                  # composable name -> [(Kt, Fun)]
        for k in self.kt:
            for f in k.funs:
                if f.composable: self.funs.setdefault(f.name, []).append((k, f))
        self.kt_loc = sum(1 for k in self.kt for ln in k.code.splitlines() if ln.strip())
        # line-count is format-dependent (a codex draft packs 13 KB into 101 lines); non-whitespace chars of comment-free code is the format-invariant size
        self.kt_nws = sum(len(re.sub(r"\s+", "", k.code)) for k in self.kt)
        self.alltext = "\n".join(k.code for k in self.kt)
        self.gradletext = "\n".join(t for _, t in self.gradle + self.toml)

    def manifests(self):
        return [(p, t) for p, t in self.xml if p.endswith("AndroidManifest.xml")]

def F(rule, file, line, evidence, **kw):
    d = {"rule_id": rule, "file": file, "line": line, "detector": "slop-scan", "evidence": " ".join(str(evidence).split())[:170], "triage": "unk"}
    d.update(kw); return d

RULES = []
def rule(*ids):
    def deco(fn): fn.ids = ids; RULES.append(fn); return fn
    return deco

def theme_file(path):
    return bool(re.search(r"(^|/)(Color|Theme|Type|Shape|Dimens?|Spacing|Typography|Colors)\.kt$|/theme/", path))

def scan(root, run_id):
    import r_perf, r_sec, r_ui, r_stale  # noqa: F401  (register rules)
    proj = Project(root)
    out, metrics = [], {"kotlin_loc": proj.kt_loc, "kotlin_nws": proj.kt_nws, "kt_files": len(proj.kt)}
    for fn in RULES:
        try:
            res = fn(proj)
        except Exception as e:                           # a rule crash must never silently zero a rule
            out.append(F("ERR", "", 0, f"{fn.__name__}: {e!r}", detector="slop-scan-error")); continue
        if isinstance(res, tuple): res, m = res; metrics.update(m)
        out += res
    for x in out: x["run_id"] = run_id
    return out, metrics

COLS = ["run_id", "rule_id", "file", "line", "detector", "evidence", "triage"]
def write_csv(rows, path, header=True):
    with open(path, "a" if not header else "w", newline="") as f:
        w = csv.DictWriter(f, COLS, extrasaction="ignore")
        if header: w.writeheader()
        w.writerows(rows)

def main():
    ap = argparse.ArgumentParser(); ap.add_argument("cmd", choices=["scan"]); ap.add_argument("dir")
    ap.add_argument("--id", required=True); ap.add_argument("--out"); ap.add_argument("--csv")
    a = ap.parse_args()
    rows, metrics = scan(a.dir, a.id)
    if a.out: pathlib.Path(a.out).write_text(json.dumps({"run_id": a.id, "metrics": metrics, "findings": rows}, indent=1))
    if a.csv: write_csv(rows, a.csv)
    from collections import Counter
    print(a.id, metrics, dict(sorted(Counter(r["rule_id"] for r in rows).items())))

if __name__ == "__main__":
    main()
