#!/usr/bin/env python3
"""E3 repair: the original scans ran jadx with -j 2 and -Xmx1g and ignored its exit status; that run is cut short (about half of the classes, e.g. 3428 of 5951 java files and 3 of 12 app classes for
codex-luna-B2-guardrailed-r2), so the jadx secret scan and the manual-triage notes that cite the jadx tree covered only part of the app code. This script redoes jadx for every target with -j 1 -Xmx1536m,
compares the app-class counts of the dex files with the java files written, reruns the secret scan on the complete tree and stores the tree as e3/archive/<target>.jadx-full.tgz.
  e3_rejadx.py [targets...]    -> results/jadx_completeness.csv, results/jadx_rows.csv (new jadx candidate rows, triage unk), results/jadx_literals.csv (credential-looking literals in app code, for the triage notes)
Every jadx run holds the shared bench lock and waits for MemAvailable >= 1500 MB; one target at a time."""
import csv, gzip, json, os, pathlib, re, shutil, subprocess, sys, time
HERE = pathlib.Path(__file__).resolve().parent
E3 = HERE.parent
sys.path.insert(0, str(HERE)); sys.path.insert(0, str(E3.parent / "e4-pressure" / "harness"))
import e3_run, gen_launch as gl
WORK = gl.WORK
R = E3 / "results"
DEXDUMP = pathlib.Path.home() / "Android/Sdk/build-tools/37.0.0/dexdump"
PRODUCT = WORK / "apk" / "shelftap-app-release-unsigned.apk"  # rebuilt 2026-11-23 from HEAD e0781e0 by beta-f2-3; the MobSF rows of this target come from the build of 2026-11-21

def sh(c): return subprocess.run(c, shell=True, capture_output=True, text=True).stdout
def apk_of(t):
    if t == "shelftap-product-release": return PRODUCT
    if t.startswith("optlab"): return WORK / "apk" / (t + ".apk")
    return WORK / "apk-corpus" / (t + ".apk")
def prefix(t, pkg):
    return "vn/shelftap" if t == "shelftap-product-release" else "shelftap/optlab" if t.startswith("optlab") else pkg.replace(".", "/")
def dex_app_classes(apk, pre):
    n = 0
    for d in sh(f"unzip -l {apk} | grep -o 'classes[0-9]*\\.dex'").split():
        n += int(sh(f"unzip -p {apk} {d} > /dev/shm/e3rj.dex; {DEXDUMP} -f /dev/shm/e3rj.dex | grep -F 'Class descriptor' | grep -F \"'L{pre}\" | grep -v -F '$' | wc -l").strip() or 0)
    if os.path.exists("/dev/shm/e3rj.dex"): os.remove("/dev/shm/e3rj.dex")
    return n
def java_app(root, pre): return sum(1 for p in (root / "sources" / pre).rglob("*.java") if "$" not in p.name) if (root / "sources" / pre).exists() else 0
def tar_java(t, pre):
    tgz = WORK / "e3" / "archive" / f"{t}.tgz"
    ls = sh(f"tar -tzf {tgz}").splitlines()
    return sum(1 for l in ls if "/jadx/sources/" in l and l.endswith(".java")), sum(1 for l in ls if f"/jadx/sources/{pre}/" in l and l.endswith(".java") and "$" not in l)

def main():
    targets = sys.argv[1:] or sorted({r["target"] for r in csv.DictReader(open(R / "findings_candidates.csv"))})
    done = {r["target"] for r in csv.DictReader(open(R / "jadx_completeness.csv"))} if (R / "jadx_completeness.csv").exists() else set()
    for t in targets:
        if t in done: print("skip", t, flush=True); continue
        pkg = json.loads(gzip.open(R / "mobsf" / f"{t}.json.gz").read()).get("package_name", ""); pre = prefix(t, pkg); apk = apk_of(t)
        old_total, old_app = tar_java(t, pre)
        e3_run.wait_mem()
        out = pathlib.Path("/dev/shm") / f"e3rj-{t}"; shutil.rmtree(out, ignore_errors=True)
        t0 = time.time()
        with gl.bench_shared():
            r = subprocess.run([str(e3_run.JADX), "-j", "1", "--no-imports", "-d", str(out), str(apk)], capture_output=True, text=True, timeout=1500, env={**os.environ, "JAVA_OPTS": "-Xmx1536m"})
        log = r.stdout + r.stderr; oom = "OutOfMemory" in log
        total = sum(1 for _ in (out / "sources").rglob("*.java")) if (out / "sources").exists() else 0
        dex_n, new_app = dex_app_classes(apk, pre), java_app(out, pre)
        errs = re.findall(r"finished with errors, count: (\d+)", log)
        row = [t, pkg, pre, dex_n, old_app, new_app, old_total, total, r.returncode, errs[0] if errs else "0", oom, round(time.time() - t0)]
        # new secret scan + credential-looking literals in app code
        hits = e3_run.secret_scan(out, pkg)
        lit = []
        for p in (out / "sources").rglob("*.java"):
            ps = p.as_posix()
            if any(("sources/" + l) in ps for l in e3_run.LIBS) and ("/" + pre + "/") not in ps: continue
            try: tx = p.read_text(errors="replace")
            except Exception: continue
            for m in re.finditer(r'(?i)\b\w*(passw(?:or)?d|secret|api[_-]?key|token)\w*\s*=\s*"([^"\n]{6,})"', tx): lit.append((p.relative_to(out).as_posix(), m.group(0)[:100]))
        with open(R / "jadx_rows.csv", "a", newline="") as fh:
            w = csv.writer(fh, lineterminator="\n")
            if fh.tell() == 0: w.writerow(["target", "name", "file", "value"])
            for n, f, v in hits: w.writerow([t, n, f, v])
        with open(R / "jadx_literals.csv", "a", newline="") as fh:
            w = csv.writer(fh, lineterminator="\n")
            if fh.tell() == 0: w.writerow(["target", "file", "literal"])
            for f, v in lit: w.writerow([t, f, v])
        tgz = WORK / "e3" / "archive" / f"{t}.jadx-full.tgz"
        subprocess.run(["tar", "--use-compress-program=gzip -1", "-cf", str(tgz), "-C", "/dev/shm", f"e3rj-{t}"], check=True)
        n_tar = len(sh(f"tar -tzf {tgz}").splitlines()); n_dir = 1 + sum(len(d) + len(f) for _, d, f in os.walk(out))
        if n_tar == n_dir: shutil.rmtree(out, ignore_errors=True)
        else: print("ARCHIVE SHORT", t, n_tar, n_dir, flush=True)
        with open(R / "jadx_completeness.csv", "a", newline="") as fh:
            w = csv.writer(fh, lineterminator="\n")
            if fh.tell() == 0: w.writerow(["target", "package", "prefix", "dex_app_toplevel_classes", "old_java_app", "new_java_app", "old_java_total", "new_java_total", "jadx_rc", "jadx_errors", "oom", "secs"])
            w.writerow(row)
        print(row, "secret hits", len(hits), "literals", len(lit), flush=True)

if __name__ == "__main__":
    main()
