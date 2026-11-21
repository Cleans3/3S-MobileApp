#!/usr/bin/env python3
"""E3: build results/findings.csv from findings_candidates.csv (+ results/triage_manual.csv for the 'unk' rows). Stdlib.
  build_findings.py         -> results/findings.csv, results/summary_e3.csv; exits 1 and lists the rows still 'unk'
triage_manual.csv columns: target,native_rule,evidence_prefix,triage,note   (triage = tp|fp; matched on target + native_rule + evidence startswith prefix)
Duplicates (same target, tool, native_rule, evidence) are collapsed. findings.csv columns: target,tool,masvs,rule_id,severity,evidence,triage,method,native_rule."""
import csv, pathlib, sys
from collections import Counter, defaultdict
R = pathlib.Path(__file__).resolve().parent.parent / "results"
COLS = ["target", "tool", "masvs", "rule_id", "severity", "evidence", "triage", "method", "native_rule"]
NORM = {  # native_rule prefix -> (taxonomy id, MASVS area) for rules whose scan-time MAP entry was blank; applied only where the candidate row has none
    "allowbackup_not_set": ("S06", "MASVS-STORAGE"),
    "explicitly_exported": ("", "MASVS-PLATFORM"), "exported_protected_permission_not_defined": ("", "MASVS-PLATFORM"),
}

def rows(p):
    return list(csv.DictReader(open(p))) if p.exists() else []

def main():
    cand, man = rows(R / "findings_candidates.csv"), rows(R / "triage_manual.csv")
    seen, out, unk = set(), [], []
    for r in cand:
        k = (r["target"], r["tool"], r["native_rule"], r["evidence"])
        if k in seen: continue
        seen.add(k)
        for pre, (rid, area) in NORM.items():
            if r["native_rule"].startswith(pre):
                if not r["masvs"]: r = {**r, "masvs": area}
                if r["rule_id"] in ("", "unmapped") and rid: r = {**r, "rule_id": rid}
        if r["triage"] == "unk":
            m = next((m for m in man if m["target"] in (r["target"], "*") and r["native_rule"].startswith(m["native_rule"]) and r["evidence"].startswith(m["evidence_prefix"])), None)
            if m: r = {**r, "triage": m["triage"], "method": "manual: " + m["note"]}
            else: unk.append(r)
        out.append(r)
    with open(R / "findings.csv", "w", newline="") as fh:
        w = csv.DictWriter(fh, COLS, lineterminator="\n"); w.writeheader(); w.writerows(out)
    by = defaultdict(Counter)
    for r in out: by[(r["target"], r["tool"])][r["triage"]] += 1
    with open(R / "summary_e3.csv", "w", newline="") as fh:
        w = csv.writer(fh, lineterminator="\n"); w.writerow(["target", "tool", "tp", "fp", "unk", "n", "precision_tp_over_tp_fp"])
        for (t, tool), c in sorted(by.items()):
            n = sum(c.values()); d = c["tp"] + c["fp"]
            w.writerow([t, tool, c["tp"], c["fp"], c["unk"], n, round(c["tp"] / d, 3) if d else ""])
    print(f"{len(out)} findings, {len(unk)} still unk")
    for r in unk: print(" unk:", r["target"], r["native_rule"], r["evidence"][:80], "|", r["method"][:90])
    sys.exit(1 if unk else 0)

if __name__ == "__main__":
    main()
