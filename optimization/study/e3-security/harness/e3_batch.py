#!/usr/bin/env python3
"""E3 batch: product release APK + the corpus survivors in results/corpus_survivors.csv, one e3_run.py call each (MobSF + jadx + secret scan, shared bench lock inside).
Skips targets that already have rows in results/findings_candidates.csv. Run in the background and end the turn; progress in <work>/e3/batch.log."""
import csv, pathlib, subprocess, sys, os
HERE = pathlib.Path(__file__).resolve().parent
E3 = HERE.parent
sys.path.insert(0, str(E3.parent / "e4-pressure" / "harness"))
import gen_launch as gl
APKS = gl.WORK / "apk-corpus"
PRODUCT = E3.parent.parent.parent / "android" / "app" / "build" / "outputs" / "apk" / "release" / "app-release-unsigned.apk"
done = {r["target"] for r in csv.DictReader(open(E3 / "results" / "findings_candidates.csv"))} if (E3 / "results" / "findings_candidates.csv").exists() else set()
jobs = [("shelftap-product-release", PRODUCT)] + [(r["run_id"], APKS / (r["run_id"] + ".apk")) for r in csv.DictReader(open(E3 / "results" / "corpus_survivors.csv"))]
env = {**os.environ, "TMPDIR": str(gl.WORK / "tmp" / "beta-f2-4")}
for target, apk in jobs:
    if target in done: print("skip", target, flush=True); continue
    print("scan", target, flush=True)
    for attempt in (1, 2, 3):  # the repo volume (ntfs-3g) returns intermittent ENOSPC on file creation; a failed scan wrote no rows, so it is simply repeated
        r = subprocess.run([sys.executable, "-I", str(HERE / "e3_run.py"), str(apk), "--target", target], env=env, capture_output=True, text=True)
        print(target, "attempt", attempt, r.returncode, (r.stdout.strip().splitlines() or [""])[-1], r.stderr.strip()[-160:].replace("\n", " "), flush=True)
        if r.returncode == 0: break
        import time; time.sleep(60)
