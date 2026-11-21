#!/usr/bin/env python3
"""Detector validation, check (a) of protocol 5: seeded-mutation recall + benign-control false-fire rate.

Clean base = android/app (hand-hardened) with a baseline profile added (so the P11 absence rule starts clean).
Each mutant is injected into a fresh copy of the base; recall = share of mutants for which the rule fires on more
(rule, file) findings than on the base. Benign controls are written as one file each; they must NOT fire their rule.
Output: results/validation/mutation.csv (per rule) and mutation_detail.csv. Rule with recall < 0.80 is unvalidated-recall (protocol 5).
"""
import collections, csv, json, pathlib, shutil, sys, tempfile

HERE = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import slopscan  # noqa: E402
E1 = HERE.parent
BASE_SRC = pathlib.Path("/mnt/01DC198BE9192500/github/Schooldev/3S-MobileApp/android/app")
TMP = pathlib.Path("/mnt/01DC198BE9192500/github/Schooldev/3S-study-work/tmp/beta-f2-2/mutation")

def fresh(dst):
    shutil.rmtree(dst, ignore_errors=True)
    shutil.copytree(BASE_SRC, dst, ignore=shutil.ignore_patterns("build", ".gradle"))
    g = dst / "build.gradle.kts"
    g.write_text(g.read_text().replace("    alias(libs.plugins.kotlin.serialization)", '    alias(libs.plugins.kotlin.serialization)\n    id("androidx.baselineprofile")', 1))
    (dst / "src/main/baseline-prof.txt").write_text("Lshelftap/android/MainActivity;\n")

def counts(root):
    rows, _ = slopscan.scan(root, "x")
    return collections.Counter((r["rule_id"], r["file"]) for r in rows), rows

def apply(dst, step, tag):
    if step["kind"] == "kt":
        d = dst / "src/main/kotlin/mutants"; d.mkdir(parents=True, exist_ok=True)
        name = step.get("name") or f"M_{tag}.kt"
        (d / name).write_text("package mutants\n\n" + step["code"] + "\n"); return f"src/main/kotlin/mutants/{name}"
    if step["kind"] == "edit":
        f = dst / step["file"]; t = f.read_text()
        assert step["old"] in t, f"{tag}: anchor not found in {step['file']}: {step['old'][:40]!r}"
        f.write_text(t.replace(step["old"], step["new"], 1)); return step["file"]
    if step["kind"] == "rm":
        (dst / step["path"]).unlink(); return step["path"]

def main():
    spec = json.loads((HERE / "mutants.json").read_text())
    base = TMP / "base"; fresh(base); base_counts, base_rows = counts(base)
    detail, per = [], collections.defaultdict(lambda: [0, 0, 0, 0])   # rule -> [mutants, fired, benign, benign_fired]
    for rule, muts in spec["mutants"].items():
        for i, m in enumerate(muts, 1):
            d = TMP / f"{rule}_{i}"; fresh(d)
            files = [apply(d, s, f"{rule}_{i}") for s in (m if isinstance(m, list) else [m])]
            c, rows = counts(d)
            fired = any(c[(rule, f)] > base_counts[(rule, f)] for f in files) or sum(v for (r, _), v in c.items() if r == rule) > sum(v for (r, _), v in base_counts.items() if r == rule)
            per[rule][0] += 1; per[rule][1] += fired
            ev = next((r["evidence"] for r in rows if r["rule_id"] == rule and c[(rule, r["file"])] > base_counts[(rule, r["file"])]), "")
            detail.append({"rule_id": rule, "kind": "mutant", "n": i, "fired": int(fired), "evidence": ev[:90]})
            shutil.rmtree(d, ignore_errors=True)
    for rule, snippets in spec["benign"].items():
        for i, code in enumerate(snippets, 1):
            d = TMP / f"B_{rule}_{i}"; fresh(d)
            f = apply(d, {"kind": "kt", "code": code}, f"B_{rule}_{i}")
            c, rows = counts(d)
            fired = any(r["rule_id"] == rule and r["file"] == f for r in rows)
            per[rule][2] += 1; per[rule][3] += fired
            detail.append({"rule_id": rule, "kind": "benign", "n": i, "fired": int(fired), "evidence": next((r["evidence"][:90] for r in rows if r["rule_id"] == rule and r["file"] == f), "")})
            shutil.rmtree(d, ignore_errors=True)
    out = E1 / "results" / "validation"; out.mkdir(parents=True, exist_ok=True)
    with open(out / "mutation.csv", "w", newline="") as f:
        w = csv.writer(f); w.writerow(["rule_id", "mutants", "fired", "recall", "benign_controls", "benign_false_fires", "status"])
        for rule in sorted(per):
            n, k, b, bf = per[rule]
            w.writerow([rule, n, k, f"{k / n:.2f}" if n else "", b, bf, ("validated-recall" if n >= 3 and k / n >= 0.8 else "unvalidated-recall")])
    with open(out / "mutation_detail.csv", "w", newline="") as f:
        w = csv.DictWriter(f, ["rule_id", "kind", "n", "fired", "evidence"]); w.writeheader(); w.writerows(detail)
    for rule in sorted(per): print(rule, per[rule], [d["n"] for d in detail if d["rule_id"] == rule and d["kind"] == "mutant" and not d["fired"]])

if __name__ == "__main__":
    main()
