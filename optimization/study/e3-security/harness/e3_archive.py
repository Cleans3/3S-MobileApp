#!/usr/bin/env python3
"""E3 housekeeping (alpha 2026-11-21, /mnt ntfs file-record exhaustion): after a scan keep ONE gzip of MobSF's report JSON in results/mobsf/ and ONE tar.gz of the whole per-scan work dir
(jadx tree + report) in <work>/archive/, then delete the tree and MobSF's upload extraction (about 28k files per scan). e3_run.py calls archive() after every scan.
  e3_archive.py --finished        archive every target listed in results/findings_candidates.csv that still has a work dir (skips --skip names)
  e3_archive.py --uploads-orphans delete MobSF upload dirs whose md5 belongs to no active scan (keeps the newest dir)"""
import argparse, csv, gzip, json, os, pathlib, shutil, subprocess, sys, time
HERE = pathlib.Path(__file__).resolve().parent
E3 = HERE.parent
sys.path.insert(0, str(E3.parent / "e4-pressure" / "harness"))
import gen_launch as gl
WORK = gl.WORK / "e3"
UPLOADS = gl.WORK / "mobsf-data" / "uploads"

def retry(fn, tries=8, wait=15):
    for i in range(tries):
        try: return fn()
        except OSError:
            if i == tries - 1: raise
            time.sleep(wait)

def count(p):
    return 1 + sum(len(d) + len(f) for _, d, f in os.walk(p))  # root + every dir + every file = the entries tar lists

def archive(target, md5=None):
    d = WORK / target
    if not d.exists(): return "no dir"
    mj = d / "mobsf.json"
    if mj.exists():
        if md5 is None:
            try: md5 = json.loads(mj.read_text()).get("md5")
            except Exception: md5 = None
        out = E3 / "results" / "mobsf" / f"{target}.json.gz"
        def w():
            out.parent.mkdir(parents=True, exist_ok=True)
            with gzip.open(out, "wb") as f: f.write(mj.read_bytes())
        retry(w)
    tgz = WORK / "archive" / f"{target}.tgz"
    def mk():
        tgz.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run(["tar", "--use-compress-program=gzip -1", "-cf", str(tgz), "-C", str(WORK), target], check=True)
    retry(mk)
    n_tar = len(subprocess.run(["tar", "-tzf", str(tgz)], capture_output=True, text=True, check=True).stdout.splitlines())
    n_dir = count(d)
    if n_tar != n_dir:  # a short archive is never followed by a delete
        raise RuntimeError(f"archive of {target} incomplete: {n_tar} < {n_dir}")
    shutil.rmtree(d, ignore_errors=True)
    if md5 and (UPLOADS / md5).exists(): shutil.rmtree(UPLOADS / md5, ignore_errors=True)
    return f"{target}: {n_dir} entries -> {tgz.name} ({tgz.stat().st_size >> 20} MB), md5 {md5 or '-'}"

if __name__ == "__main__":
    ap = argparse.ArgumentParser(); ap.add_argument("--finished", action="store_true"); ap.add_argument("--uploads-orphans", action="store_true"); ap.add_argument("--skip", nargs="*", default=[])
    ap.add_argument("targets", nargs="*"); a = ap.parse_args()
    done = {r["target"] for r in csv.DictReader(open(E3 / "results" / "findings_candidates.csv"))}
    for t in (a.targets or (sorted(done) if a.finished else [])):
        if t in a.skip: continue
        if t in done or a.targets: print(archive(t), flush=True)
    if a.uploads_orphans and UPLOADS.exists():
        ds = sorted(UPLOADS.iterdir(), key=lambda p: p.stat().st_mtime)
        for p in ds[:-1]: shutil.rmtree(p, ignore_errors=True); print("removed upload", p.name, flush=True)
