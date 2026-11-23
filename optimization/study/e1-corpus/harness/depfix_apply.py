#!/usr/bin/env python3
"""Replace the depcheck rows (S11, D02) of the first-scan findings with the re-resolved rows in results/depfix/*.json (alpha Q#dep-fix).

First-scan depcheck stopped at the first declared repo that answered (a stale google() KSP plugin marker hid versions that exist on Central and the Plugin Portal).
The fixed lookup unions the declared repos (slop-scan/depcheck.py check()); harness re-resolve script writes results/depfix/*. This script only swaps rows:
  results/validation/v1/pre_fix_findings.csv + pre_fix_human_findings.csv -> results/depfix/findings_v1_corrected.csv, human_findings_v1_corrected.csv
  results/findings.csv + human_findings.csv                               -> results/depfix/findings_v2_corrected.csv, human_findings_v2_corrected.csv
"""
import csv, json, pathlib, collections

R = pathlib.Path(__file__).resolve().parents[1] / "results"; D = R / "depfix"
COLS = ["run_id", "rule_id", "file", "line", "detector", "evidence", "triage"]

def fixed(name):
    out = {}
    for e in json.loads((D / name).read_text()): out[e["run_id"]] = e["findings"]
    return out

def swap(src, fx, dst):
    rows = list(csv.DictReader(open(src)))
    keep = [r for r in rows if r["detector"] != "depcheck"]
    new = [r for rid, fs in fx.items() for r in fs]
    old_dep = collections.Counter((r["rule_id"]) for r in rows if r["detector"] == "depcheck"); new_dep = collections.Counter(r["rule_id"] for r in new)
    out = sorted(keep + new, key=lambda x: (x["run_id"], x["rule_id"], x["file"], int(x["line"] or 0)))
    with open(dst, "w", newline="") as f:
        w = csv.DictWriter(f, COLS, extrasaction="ignore"); w.writeheader(); w.writerows(out)
    print(dst.name, "depcheck rows first", dict(old_dep), "-> fixed", dict(new_dep))

def main():
    fr, fh = fixed("findings_fixed.json"), fixed("human_findings_fixed.json")
    V = R / "validation" / "v1"
    swap(V / "pre_fix_findings.csv", fr, D / "findings_v1_corrected.csv"); swap(V / "pre_fix_human_findings.csv", fh, D / "human_findings_v1_corrected.csv")
    swap(R / "findings.csv", fr, D / "findings_v2_corrected.csv"); swap(R / "human_findings.csv", fh, D / "human_findings_v2_corrected.csv")

if __name__ == "__main__":
    main()
