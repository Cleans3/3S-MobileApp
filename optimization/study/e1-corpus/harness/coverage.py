#!/usr/bin/env python3
"""Coverage matrix (protocol 5c): which taxonomy rule is reported by slop-scan, Android Lint, detekt compose-rules (+ depcheck).

Declared coverage comes from COVER (tool documentation); observed counts come from findings.csv over generated runs. A rule is 'custom-only'
when no existing tool declares it; then the custom rule stays. Where an existing tool covers it, its finding is credited in the report.
"""
import collections, csv, pathlib
E1 = pathlib.Path(__file__).resolve().parents[1]
COVER = {  # rule -> {tool: [issue ids]}  (Android Lint issue ids from build_run.LINT_MAP; detekt compose-rules 0.6.8 rule names)
    "P03": {"detekt": ["UnstableCollections", "MutableParams", "MutableStateParam", "StateParam"]}, "P04": {"detekt": ["RememberMissing", "RememberContentMissing"]},
    "P05": {"lint": ["FrequentlyChangingValue"]}, "S03": {"lint": ["UsesCleartextTraffic", "InsecureBaseConfiguration"]},
    "S04": {"lint": ["TrustAllX509TrustManager", "BadHostnameVerifier", "AllowAllHostnameVerifier", "CustomX509TrustManager"]},
    "S06": {"lint": ["AllowBackup", "DataExtractionRules", "FullBackupContent"]}, "S07": {"lint": ["ExportedActivity", "ExportedService", "ExportedReceiver", "ExportedContentProvider"]},
    "S08": {"lint": ["GetInstance", "DeprecatedProvider"]}, "S09": {"lint": ["SetJavaScriptEnabled", "JavascriptInterface"]},
    "S11": {"depcheck": ["Maven Central / Google Maven existence + OSV"]}, "D01": {"lint": ["Deprecated", "GradleDeprecated"]}, "D02": {"lint": ["GradleDependency", "NewerVersionAvailable"], "depcheck": ["latest-stable"]},
    "U01": {"lint": ["ContentDescription (views only)"]},
}
MEASURED = ["P01", "P02", "P03", "P04", "P05", "P06", "P07", "P08", "P09", "P10", "P11", "P13", "S01", "S02", "S03", "S04", "S05", "S06", "S07", "S08", "S09", "S11",
            "U01", "U02", "U03", "U04", "U05", "D01", "D02"]
def main():
    obs = collections.defaultdict(collections.Counter)
    f = E1 / "results" / "findings.csv"
    if f.exists():
        for r in csv.DictReader(open(f)): obs[r["rule_id"]][r["detector"]] += 1
    rows = []
    for rule in MEASURED:
        tools = COVER.get(rule, {})
        rows.append({"rule_id": rule, "slop-scan_findings": obs[rule]["slop-scan"], "lint_findings": obs[rule]["lint"], "depcheck_findings": obs[rule]["depcheck"],
                     "existing_tool_declares": ";".join(f"{t}:{'|'.join(v)}" for t, v in tools.items()) or "none", "custom_only": int(not tools)})
    detekt = sum(v for k, c in obs.items() for d, v in c.items() if d == "detekt")
    with open(E1 / "results" / "validation" / "coverage_matrix.csv", "w", newline="") as fh:
        w = csv.DictWriter(fh, list(rows[0])); w.writeheader(); w.writerows(rows)
    print(len(rows), "rules;", sum(r["custom_only"] for r in rows), "custom-only;", "detekt compose findings (all rule names):", detekt)
if __name__ == "__main__": main()
