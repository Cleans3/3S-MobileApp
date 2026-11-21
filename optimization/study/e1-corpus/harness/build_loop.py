#!/usr/bin/env python3
"""Interleaved serial builder: build every valid non-pilot run that has no build.json yet, then wait for new runs until the campaign is done.
Each build_run.py call takes gradle.lock itself (shared with f2-3). Stops when no campaign process is left and nothing is left to build.

  python3 harness/build_loop.py
"""
import json, pathlib, subprocess, sys, time

E1 = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(E1.parent / "e4-pressure" / "harness"))
import gen_launch  # noqa: E402  (read-only reuse; bench_shared waits on WORK/bench.hold and holds bench.lock shared, as generations do)
HERE = pathlib.Path(__file__).resolve().parent


def campaign_alive():
    return subprocess.run(["pgrep", "-f", "generate.py campaign"], capture_output=True).returncode == 0



def todo():
    out = []
    for d in sorted((E1 / "runs").glob("*")):
        if d.name.startswith("pilot-") or (d / "build.json").exists() or not (d / "meta.json").exists():
            continue
        if subprocess.run(["pgrep", "-f", f"build_[r]un.py {d.name}$"], capture_output=True).returncode == 0:
            continue                                            # a build_run from an earlier loop is still on this run
        if json.loads((d / "meta.json").read_text()).get("valid"):
            out.append(d.name)
    return out


while True:
    t = todo()
    if not t:
        if not campaign_alive():
            break
        time.sleep(60); continue
    rid = t[0]
    with gen_launch.bench_shared():
        r = subprocess.run([sys.executable, str(HERE / "build_run.py"), rid], capture_output=True, text=True)
    print(rid, "exit", r.returncode, (r.stdout.strip().splitlines() or [""])[-1][:160], flush=True)
    if not (E1 / "runs" / rid / "build.json").exists():            # build_run crashed before writing build.json: record it once, retry later runs
        for _ in range(5):
            try:
                (E1 / "runs" / rid / "build.json").write_text(json.dumps({"build": "harness-error", "build_error_first_line": r.stderr.strip()[-200:]})); break
            except OSError:                                         # disk-full style failure: wait and try again, never die with runs unbuilt
                time.sleep(60)
        else:
            sys.exit("cannot write build.json (volume full?)")
print("BUILD LOOP DONE", flush=True)
