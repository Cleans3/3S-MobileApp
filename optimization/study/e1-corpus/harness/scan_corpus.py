#!/usr/bin/env python3
"""Scan generated runs and the human corpus with slop-scan + depcheck + detekt; rebuild the CSVs.

  scan_corpus.py runs  [--only RUN_ID ...]   -> results/findings.csv, results/metrics.csv, results/coordinates.csv
  scan_corpus.py human                       -> results/human_findings.csv, results/human_metrics.csv
Per-run raw detector output is cached in runs/<id>/scan.json (so reruns after a rule change are explicit: --force).
"""
import argparse, csv, json, pathlib, sys

HERE = pathlib.Path(__file__).resolve().parent
E1 = HERE.parent
sys.path.insert(0, str(E1 / "slop-scan")); sys.path.insert(0, str(HERE))
import slopscan, depcheck, detekt_run  # noqa: E402

WORK = pathlib.Path("/mnt/01DC198BE9192500/github/Schooldev/3S-study-work")
COLS = slopscan.COLS

def scan_one(src, rid, snap=True, with_detekt=True):
    rows, metrics = slopscan.scan(src, rid)
    dep = depcheck.run(src, rid, E1 / "results" / "maven-snapshot" if snap else None)
    rows += dep["findings"]
    det = []
    if with_detekt:
        try: det, rc, err = detekt_run.run(src, rid)
        except Exception as e: det = []; metrics["detekt_error"] = repr(e)
    return {"run_id": rid, "metrics": metrics, "findings": rows, "detekt": det, "coordinates": dep["coordinates"], "dep_date": dep["date"]}

def write_all(results, fcsv, mcsv, ccsv=None):
    with open(fcsv, "w", newline="") as f:
        w = csv.DictWriter(f, COLS, extrasaction="ignore"); w.writeheader()
        for r in results:
            w.writerows(sorted(r["findings"] + r["detekt"] + r.get("lint", []), key=lambda x: (x["rule_id"], x["file"], int(x["line"] or 0))))
    keys = sorted({k for r in results for k in r["metrics"]})
    with open(mcsv, "w", newline="") as f:
        w = csv.DictWriter(f, ["run_id"] + keys); w.writeheader()
        for r in results: w.writerow({"run_id": r["run_id"], **r["metrics"]})
    if ccsv:
        with open(ccsv, "w", newline="") as f:
            w = csv.writer(f); w.writerow(["run_id", "kind", "group", "artifact", "version", "class", "latest_stable", "major_behind", "gap_months", "vulns", "file", "line", "dep_date"])
            for r in results:
                for c in r["coordinates"]:
                    w.writerow([r["run_id"], c["kind"], c["group"], c["artifact"], c["version"] or "", c["class"], c.get("latest_stable") or "", c.get("major_behind", ""), c.get("gap_months", ""), ";".join(c.get("vulns") or []), c["file"], c["line"], r["dep_date"]])

def main():
    ap = argparse.ArgumentParser(); ap.add_argument("what", choices=["runs", "human", "ref"]); ap.add_argument("--only", nargs="*"); ap.add_argument("--force", action="store_true")
    a = ap.parse_args(); res = E1 / "results"
    if a.what == "runs":
        results = []
        for d in sorted((E1 / "runs").glob("*/meta.json")):
            run = d.parent; rid = run.name
            if a.only and rid not in a.only and not a.force and (run / "scan.json").exists(): results.append(json.loads((run / "scan.json").read_text())); continue
            cache = run / "scan.json"
            if cache.exists() and not a.force and not a.only: r = json.loads(cache.read_text())
            else:
                r = scan_one(run / "src", rid); cache.write_text(json.dumps(r))
            lf = run / "lint_findings.json"
            r["lint"] = json.loads(lf.read_text()) if lf.exists() else []
            results.append(r)
        write_all([r for r in results if not r["run_id"].startswith("pilot-")], res / "findings.csv", res / "metrics.csv", res / "coordinates.csv")
        write_all([r for r in results if r["run_id"].startswith("pilot-")], res / "pilot_findings.csv", res / "pilot_metrics.csv", res / "pilot_coordinates.csv")
        print(len(results), "runs scanned")
    elif a.what == "ref":
        r = scan_one(pathlib.Path("/mnt/01DC198BE9192500/github/Schooldev/3S-MobileApp/android/app"), "ref-android-app", snap=False)
        write_all([r], res / "ref_findings.csv", res / "ref_metrics.csv")
    else:
        rows = list(csv.DictReader(open(res / "human_corpus.csv")))
        results = []
        for h in rows:
            rid = "human:" + h["repo"] + "@" + h["commit"][:8]
            cache = WORK / "human-corpus" / (h["repo"].replace("/", "__") + ".scan.json")
            if cache.exists() and not a.force: results.append(json.loads(cache.read_text())); continue
            r = scan_one(WORK / "human-corpus" / h["repo"].replace("/", "__"), rid, snap=False); r["metrics"].update({"stratum": h["stratum"]})
            cache.write_text(json.dumps(r)); results.append(r); print(rid, r["metrics"]["kotlin_loc"], flush=True)
        write_all(results, res / "human_findings.csv", res / "human_metrics.csv", res / "human_coordinates.csv")
        print(len(results), "human apps scanned")

if __name__ == "__main__":
    main()
