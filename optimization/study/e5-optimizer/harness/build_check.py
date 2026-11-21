#!/usr/bin/env python3
"""Compile check of an E5 work copy (or the pristine target): reuses e1-corpus/harness/build_run.py read-only (JDK, SDK, GRADLE_USER_HOME, wrapper, heap caps).
  build_check.py DIR [--tasks :app:compileDebugKotlin]   -> prints {"rc":..,"seconds":..,"first_error":..}; builds in a copy under 3S-study-work/build/e5/, holds the shared bench lock and gradle.lock."""
import argparse, json, pathlib, sys, hashlib, time
HERE = pathlib.Path(__file__).resolve().parent
E5 = HERE.parent
sys.path.insert(0, str(E5.parent / "e1-corpus" / "harness")); sys.path.insert(0, str(E5.parent / "e4-pressure" / "harness"))
import build_run as br, gen_launch as gl

def check(src, tasks=(":app:compileDebugKotlin",)):
    src = pathlib.Path(src).resolve()
    dst = gl.WORK / "build" / "e5" / hashlib.sha1(str(src).encode()).hexdigest()[:10]
    dst.parent.mkdir(parents=True, exist_ok=True)
    INFRA = ("Unable to start the daemon process", "Gradle could not start your build", "No space left on device", "Could not create service", "Timeout waiting to lock")
    for attempt in (1, 2, 3):  # intermittent ENOSPC on the ntfs repo volume / resource pressure: a build that never started is not a compile failure
        try: br.prepare(src, dst)
        except OSError: time.sleep(30); continue
        with gl.bench_shared():
            rc, out, sec = br.gradle(dst, list(tasks))
        if rc == 0 or not any(s in out for s in INFRA): break
        time.sleep(45)
    infra = rc != 0 and any(s in out for s in INFRA)
    res = {"rc": rc, "infra": infra, "seconds": sec, "first_error": br.first_error(out) if rc else None, "tail": out.strip().splitlines()[-4:]}
    import shutil; shutil.rmtree(dst, ignore_errors=True)
    return res

if __name__ == "__main__":
    ap = argparse.ArgumentParser(); ap.add_argument("dir"); ap.add_argument("--tasks", default=":app:compileDebugKotlin")
    a = ap.parse_args(); print(json.dumps(check(a.dir, a.tasks.split(",")), indent=1))
