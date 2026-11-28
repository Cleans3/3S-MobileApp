#!/usr/bin/env python3
"""run_e7a.py — Phase 2 E7a harness: build & measure amplified E6 arm-O apps on AVD lowend34.

Follows NOTE.md §6.1 pre-stated protocol:
  - 5 launching E6 arm-O apps: claude-sonnet-O-r3, claude-haiku-O-r1, codex-luna-O-r3,
    agy-g38flash-O-r1, agy-g38flash-O-r2
  - 3 scale levels per app: orig (N0 = 8..12), n2000 (N = 2,000), n100000 (N = 100,000)
  - Amplifier: scale_probe.amplify_project(..., emit_gate=False) — zero other source edits
  - Builds: E1/E6 build_run.py environment (JDK 17/21, shared gradle-home, R7 mechanical
    table repair logged transparently if original E6 app needed one). Heavy state stays in
    3S-study-work/e7/.
  - Emulator: AVD lowend34 (-gpu swangle_indirect, MemorySwapMax=0), 5 cold-start iterations
    per (app, scale), identical launch -> meminfo/VmHWM -> 12-swipe scroll -> search probe
    -> meminfo/VmHWM procedure. Emulator is always shut down in finally.

Usage:
  python3 optimization/study/e7-novel/run_e7a.py build
  python3 optimization/study/e7-novel/run_e7a.py measure [--iters 5]
  python3 optimization/study/e7-novel/run_e7a.py summarize
"""
from __future__ import annotations

import argparse
import csv
import fcntl
import json
import os
import pathlib
import re
import shutil
import statistics
import subprocess
import sys
import time

sys.dont_write_bytecode = True

HERE = pathlib.Path(__file__).resolve().parent
STUDY = HERE.parent
WORK = pathlib.Path("/mnt/01DC198BE9192500/github/Schooldev/3S-study-work")
E6_RUNS = WORK / "e6" / "runs"
E6_APKS = WORK / "apk-corpus"
E7_WORK = WORK / "e7"
E7_AMP = E7_WORK / "amp"
E7_BUILD = E7_WORK / "build"
E7_APKS = E7_WORK / "apks"
RES_DIR = HERE / "results"

for p in (HERE, STUDY / "e1-corpus" / "harness", STUDY / "e1-corpus" / "slop-scan"):
    sys.path.insert(0, str(p))
import build_run  # noqa: E402
import scale_probe  # noqa: E402

APPS = [
    ("claude-sonnet-O-r3", 12),
    ("claude-haiku-O-r1", 12),
    ("codex-luna-O-r3", 8),
    ("agy-g38flash-O-r1", 10),
    ("agy-g38flash-O-r2", 8),
]
SCALES = [("orig", 0), ("n2000", 2000), ("n100000", 100000)]

ADB = str(pathlib.Path.home() / "Android/Sdk/platform-tools/adb")
EMU = str(pathlib.Path.home() / "Android/Sdk/emulator/emulator")
APKANALYZER = str(pathlib.Path.home() / "Android/Sdk/cmdline-tools/latest/bin/apkanalyzer")
SERIAL, PORT = "emulator-5570", "5570"
AVD_HOME = WORK / "avd"
EMU_FLAGS = [
    "-avd", "lowend34", "-no-snapshot", "-no-boot-anim", "-no-audio",
    "-no-window", "-memory", "2048", "-cores", "2", "-gpu", "swangle_indirect", "-port", PORT,
]


def cmd_build() -> int:
    E7_AMP.mkdir(parents=True, exist_ok=True)
    E7_BUILD.mkdir(parents=True, exist_ok=True)
    E7_APKS.mkdir(parents=True, exist_ok=True)
    RES_DIR.mkdir(parents=True, exist_ok=True)
    (WORK / "tmp" / "beta-f2-2").mkdir(parents=True, exist_ok=True)
    build_run.compose_sdk()
    build_run.GRADLE_HOME.mkdir(exist_ok=True)

    rows = []
    for run_id, orig_n in APPS:
        orig_apk_src = E6_APKS / f"{run_id}.apk"
        orig_apk_dst = E7_APKS / f"{run_id}-orig.apk"
        if orig_apk_src.exists():
            shutil.copyfile(orig_apk_src, orig_apk_dst)
            rows.append({
                "run_id": run_id, "scale": "orig", "target_n": orig_n,
                "amp_status": "ORIGINAL", "build_zero_touch": "reused-e6",
                "build_final": "pass", "repairs": 0, "repair_detail": "reused E6 baseline APK",
                "gradle_s": 0.0, "apk_mb": round(orig_apk_dst.stat().st_size / 1e6, 2),
                "error": "",
            })
            print(f"[BUILD] {run_id} orig -> reused E6 APK ({orig_apk_dst.stat().st_size/1e6:.2f} MB)", flush=True)

        for scale_tag, target_n in SCALES[1:]:
            amp_dir = E7_AMP / f"{run_id}-{scale_tag}"
            bld_dir = E7_BUILD / f"{run_id}-{scale_tag}"
            apk_dst = E7_APKS / f"{run_id}-{scale_tag}.apk"

            # Reuse already-built APK if present
            if apk_dst.exists():
                rows.append({
                    "run_id": run_id, "scale": scale_tag, "target_n": target_n,
                    "amp_status": "AMPLIFIED", "build_zero_touch": "cached",
                    "build_final": "pass", "repairs": 0, "repair_detail": "cached E7 APK",
                    "gradle_s": 0.0, "apk_mb": round(apk_dst.stat().st_size / 1e6, 2),
                    "error": "",
                })
                print(f"[BUILD] {run_id} {scale_tag} -> cached APK", flush=True)
                continue

            amp_meta = scale_probe.amplify_project(E6_RUNS / run_id / "src", amp_dir, target_n=target_n, emit_gate=False)
            if amp_meta["status"] != "AMPLIFIED":
                rows.append({
                    "run_id": run_id, "scale": scale_tag, "target_n": target_n,
                    "amp_status": amp_meta["status"], "build_zero_touch": "fail",
                    "build_final": "fail", "repairs": 0, "repair_detail": "",
                    "gradle_s": 0.0, "apk_mb": "", "error": amp_meta.get("reason", "amp failed"),
                })
                continue

            build_run.prepare(amp_dir, bld_dir)
            rc, out, dur = build_run.gradle(bld_dir, ["assembleDebug"])
            apk = next(iter(bld_dir.rglob("outputs/apk/debug/*.apk")), None)
            zt = "pass" if apk else "fail"
            repairs_applied = []
            step = 0
            while not apk and step < 3:
                r = build_run.repair(bld_dir, out)
                if not r:
                    break
                step += 1
                repairs_applied.append(f"{r[0]}:{r[1]}")
                rc, out, d2 = build_run.gradle(bld_dir, ["assembleDebug"])
                dur = round(dur + d2, 1)
                apk = next(iter(bld_dir.rglob("outputs/apk/debug/*.apk")), None)

            err = "" if apk else build_run.first_error(out)
            apk_mb = ""
            if apk:
                shutil.copyfile(apk, apk_dst)
                apk_mb = round(apk_dst.stat().st_size / 1e6, 2)
            # Clean heavy build tree & gradle transforms immediately (NTFS inode safety)
            shutil.rmtree(bld_dir, ignore_errors=True)
            build_run.prune_gradle_home()

            row = {
                "run_id": run_id, "scale": scale_tag, "target_n": target_n,
                "amp_status": amp_meta["status"], "build_zero_touch": zt,
                "build_final": "pass" if apk else "fail", "repairs": len(repairs_applied),
                "repair_detail": "; ".join(repairs_applied), "gradle_s": dur,
                "apk_mb": apk_mb, "error": err[:200],
            }
            rows.append(row)
            print(f"[BUILD] {run_id} {scale_tag}: zt={zt} final={row['build_final']} repairs={row['repairs']} ({dur}s) {err[:90]}", flush=True)

    with open(RES_DIR / "e7a_builds.csv", "w", newline="") as f:
        w = csv.DictWriter(f, list(rows[0].keys()))
        w.writeheader()
        w.writerows(rows)
    return 0


def adb(*args, timeout=90):
    r = subprocess.run([ADB, "-s", SERIAL, *args], capture_output=True, timeout=timeout)
    return r.stdout.decode(errors="replace") + r.stderr.decode(errors="replace")


def emu_up():
    subprocess.run([ADB, "start-server"], capture_output=True)
    log = open(E7_WORK / "emulator.log", "a")
    p = subprocess.Popen(
        ["systemd-run", "--user", "--scope", "-p", "MemorySwapMax=0", EMU, *EMU_FLAGS],
        stdout=log, stderr=log, stdin=subprocess.DEVNULL,
        env={**os.environ, "ANDROID_SDK_ROOT": str(pathlib.Path.home() / "Android/Sdk"), "ANDROID_AVD_HOME": str(AVD_HOME)},
        start_new_session=True,
    )
    t0 = time.time()
    while time.time() - t0 < 360:
        st = subprocess.run([ADB, "-s", SERIAL, "get-state"], capture_output=True, text=True).stdout
        if "device" in st and adb("shell", "getprop", "sys.boot_completed", timeout=15).strip() == "1":
            time.sleep(8)
            return p
        time.sleep(5)
    p.terminate()
    raise SystemExit("emulator did not boot in 360 s")


def emu_down(p):
    subprocess.run([ADB, "-s", SERIAL, "emu", "kill"], capture_output=True)
    try:
        p.wait(30)
    except Exception:
        subprocess.run(["pkill", "-f", f"qemu-system.*-port {PORT}"], capture_output=True)
    time.sleep(2)


def read_memory(pkg: str) -> dict:
    mem = adb("shell", "dumpsys", "meminfo", pkg, timeout=30)
    pss_m = re.search(r"TOTAL PSS:\s*(\d+)", mem) or re.search(r"^\s*TOTAL\s+(\d+)", mem, re.M)
    rss_m = re.search(r"TOTAL RSS:\s*(\d+)", mem)
    jh_m = re.search(r"Java Heap:\s*(\d+)", mem)
    pss_kb = int(pss_m.group(1)) if pss_m else None
    rss_kb = int(rss_m.group(1)) if rss_m else None
    jh_kb = int(jh_m.group(1)) if jh_m else None

    vmhwm_kb = None
    pid = adb("shell", "pidof", pkg, timeout=15).strip().split()
    if pid and pid[0].isdigit():
        st = adb("shell", "cat", f"/proc/{pid[0]}/status", timeout=15)
        hm = re.search(r"VmHWM:\s*(\d+)\s*kB", st)
        if hm:
            vmhwm_kb = int(hm.group(1))
    return {"pss_kb": pss_kb, "rss_kb": rss_kb, "java_heap_kb": jh_kb, "vmhwm_kb": vmhwm_kb}


def check_crash(pkg: str) -> tuple[bool, str]:
    log = adb("logcat", "-d", "-b", "crash", timeout=20)
    oom = "OutOfMemoryError" in log
    fatal = re.search(r"FATAL EXCEPTION.*?\n.*?Process:\s*" + re.escape(pkg), log, re.S) is not None
    anr = bool(re.search(r"ANR in " + re.escape(pkg), adb("logcat", "-d", "-t", "200", timeout=20)))
    alive = bool(adb("shell", "pidof", pkg, timeout=15).strip())
    if oom:
        return True, "OOM"
    if anr:
        return True, "ANR"
    if fatal:
        return True, "FATAL"
    if not alive:
        return True, "DEAD"
    return False, ""


def find_search_box() -> tuple[int, int] | None:
    """Inspect current UI hierarchy via uiautomator dump and locate an EditText/Search box."""
    adb("shell", "uiautomator", "dump", "/sdcard/e7_ui.xml", timeout=20)
    xml = adb("shell", "cat", "/sdcard/e7_ui.xml", timeout=20)
    for m in re.finditer(r'<node\b([^>]+)>', xml):
        attrs = m.group(1)
        is_edit = 'class="android.widget.EditText"' in attrs or 'editable="true"' in attrs
        is_search = bool(re.search(r'search|tìm', attrs, re.I))
        if is_edit or (is_search and 'clickable="true"' in attrs):
            bm = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', attrs)
            if bm:
                x1, y1, x2, y2 = map(int, bm.groups())
                if x2 > x1 and y2 > y1:
                    return ((x1 + x2) // 2, (y1 + y2) // 2)
    return None


def parse_gfxinfo(pkg: str) -> dict:
    g = adb("shell", "dumpsys", "gfxinfo", pkg, timeout=25)
    tf = re.search(r"Total frames rendered:\s*(\d+)", g)
    jk = re.search(r"Janky frames:\s*(\d+)\s*\(([\d.]+)%\)", g)
    p50 = re.search(r"50th percentile:\s*(\d+)ms", g)
    p90 = re.search(r"90th percentile:\s*(\d+)ms", g)
    p99 = re.search(r"99th percentile:\s*(\d+)ms", g)
    fs = adb("shell", "dumpsys", "gfxinfo", pkg, "framestats", timeout=25)
    durs, idx = [], None
    for ln in fs.splitlines():
        c = [x.strip() for x in ln.split(",")]
        if c and c[0] == "Flags":
            idx = (c.index("IntendedVsync"), c.index("FrameCompleted")) if "FrameCompleted" in c else None
            continue
        if idx and len(c) > idx[1] and c[0] == "0" and c[idx[0]].isdigit() and c[idx[1]].isdigit():
            d = (int(c[idx[1]]) - int(c[idx[0]])) / 1e6
            if d > 0:
                durs.append(d)
    durs.sort()
    return {
        "frames_total": int(tf.group(1)) if tf else "",
        "janky_pct": float(jk.group(2)) if jk else "",
        "p50_ms": int(p50.group(1)) if p50 else "",
        "p90_ms": int(p90.group(1)) if p90 else "",
        "p99_ms": int(p99.group(1)) if p99 else "",
        "frame_ms_med": round(durs[len(durs) // 2], 1) if durs else "",
        "frame_ms_p90": round(durs[int(len(durs) * 0.9)], 1) if durs else "",
    }


def kb_to_mb(v):
    return round(v / 1024.0, 1) if v is not None else ""


def max_opt(a, b):
    xs = [x for x in (a, b) if x is not None]
    return max(xs) if xs else None


def measure_variant(run_id: str, scale_tag: str, target_n: int, iters: int) -> list[dict]:
    apk = E7_APKS / f"{run_id}-{scale_tag}.apk"
    if not apk.exists():
        return [{"run_id": run_id, "scale": scale_tag, "target_n": target_n, "iter": 0, "status": "NO_APK"}]

    pkg = subprocess.run([APKANALYZER, "manifest", "application-id", str(apk)], capture_output=True, text=True).stdout.strip()
    adb("uninstall", pkg)
    inst = adb("install", "-r", "-t", str(apk), timeout=180)
    if "Success" not in inst:
        return [{"run_id": run_id, "scale": scale_tag, "target_n": target_n, "iter": 0, "status": "INSTALL_FAIL", "crash_reason": inst.strip()[-120:]}]

    comp = adb("shell", "cmd", "package", "resolve-activity", "--brief", "-c", "android.intent.category.LAUNCHER", pkg).strip().splitlines()[-1].strip()
    adb("shell", "pm", "grant", pkg, "android.permission.POST_NOTIFICATIONS")

    rows = []
    search_xy = None
    for it in range(1, iters + 1):
        adb("shell", "am", "force-stop", pkg)
        adb("logcat", "-c")
        time.sleep(1.0)

        t_start = time.time()
        try:
            out = adb("shell", "am", "start", "-W", "-n", comp, timeout=60)
            launch_wall_ms = round((time.time() - t_start) * 1000, 1)
        except subprocess.TimeoutExpired:
            rows.append({
                "run_id": run_id, "scale": scale_tag, "target_n": target_n, "iter": it,
                "package": pkg, "status": "LAUNCH_TIMEOUT", "crash_reason": "am start -W >60s",
                "launch_ms": 60000, "launch_wall_ms": 60000,
            })
            adb("shell", "am", "force-stop", pkg)
            continue

        mt = re.search(r"TotalTime:\s*(\d+)", out)
        launch_ms = int(mt.group(1)) if mt else ""
        time.sleep(4.0)
        dead, reason = check_crash(pkg)
        if dead:
            rows.append({
                "run_id": run_id, "scale": scale_tag, "target_n": target_n, "iter": it,
                "package": pkg, "status": "CRASH_LAUNCH", "crash_reason": reason,
                "launch_ms": launch_ms, "launch_wall_ms": launch_wall_ms,
            })
            continue

        m1 = read_memory(pkg)

        # Scroll gesture (same 12-swipe pattern as E6)
        adb("shell", "dumpsys", "gfxinfo", pkg, "reset")
        swipes = "; ".join(
            "input swipe 360 1000 360 350 300; sleep 0.5" if i % 2 == 0 else "input swipe 360 350 360 1000 300; sleep 0.5"
            for i in range(12)
        )
        adb("shell", swipes, timeout=120)
        time.sleep(1.0)
        gfx = parse_gfxinfo(pkg)
        m2 = read_memory(pkg)
        dead2, reason2 = check_crash(pkg)

        # Search probe (discover box once on iter 1)
        search_tested = 0
        search_p90_ms = ""
        search_jank_pct = ""
        search_wall_ms = ""
        m3 = {"pss_kb": None, "rss_kb": None, "java_heap_kb": None, "vmhwm_kb": None}
        if not dead2:
            if it == 1:
                search_xy = find_search_box()
            if search_xy is not None:
                search_tested = 1
                adb("shell", "dumpsys", "gfxinfo", pkg, "reset")
                adb("shell", "input", "tap", str(search_xy[0]), str(search_xy[1]))
                time.sleep(0.4)
                ts0 = time.time()
                adb("shell", "input", "text", "Orchard")
                time.sleep(1.5)
                search_wall_ms = round((time.time() - ts0 - 1.5) * 1000, 1)
                sg = parse_gfxinfo(pkg)
                search_p90_ms = sg["frame_ms_p90"] if sg["frame_ms_p90"] != "" else sg["p90_ms"]
                search_jank_pct = sg["janky_pct"]
                m3 = read_memory(pkg)
                dead3, reason3 = check_crash(pkg)
                if dead3:
                    dead2, reason2 = dead3, f"SEARCH_{reason3}"

        peak_pss = max_opt(max_opt(m1["pss_kb"], m2["pss_kb"]), m3["pss_kb"])
        peak_rss = max_opt(max_opt(m1["rss_kb"], m2["rss_kb"]), m3["rss_kb"])
        peak_jh = max_opt(max_opt(m1["java_heap_kb"], m2["java_heap_kb"]), m3["java_heap_kb"])
        peak_hwm = max_opt(max_opt(m1["vmhwm_kb"], m2["vmhwm_kb"]), m3["vmhwm_kb"])

        row = {
            "run_id": run_id, "scale": scale_tag, "target_n": target_n, "iter": it,
            "package": pkg, "status": "CRASH_RUN" if dead2 else "OK", "crash_reason": reason2,
            "launch_ms": launch_ms, "launch_wall_ms": launch_wall_ms,
            "pss_mb": kb_to_mb(peak_pss), "rss_mb": kb_to_mb(peak_rss),
            "java_heap_mb": kb_to_mb(peak_jh), "vmhwm_mb": kb_to_mb(peak_hwm),
            "frames_total": gfx["frames_total"], "janky_pct": gfx["janky_pct"],
            "p90_ms": gfx["p90_ms"], "frame_ms_med": gfx["frame_ms_med"], "frame_ms_p90": gfx["frame_ms_p90"],
            "search_tested": search_tested, "search_p90_ms": search_p90_ms,
            "search_jank_pct": search_jank_pct, "search_wall_ms": search_wall_ms,
        }
        rows.append(row)
        print(
            f"[MEAS] {run_id:18s} {scale_tag:7s} #{it}: {row['status']:10s} launch={row['launch_ms']}ms "
            f"pss={row['pss_mb']}MB rss={row['rss_mb']}MB hwm={row['vmhwm_mb']}MB heap={row['java_heap_mb']}MB "
            f"jank={row['janky_pct']}% sp90={row['search_p90_ms']}ms",
            flush=True,
        )

    adb("uninstall", pkg)
    return rows


def cmd_measure(iters: int) -> int:
    E7_WORK.mkdir(parents=True, exist_ok=True)
    RES_DIR.mkdir(parents=True, exist_ok=True)
    all_rows = []

    p = emu_up()
    try:
        for run_id, orig_n in APPS:
            for scale_tag, target_n in SCALES:
                tn = orig_n if scale_tag == "orig" else target_n
                try:
                    rows = measure_variant(run_id, scale_tag, tn, iters)
                except Exception as e:
                    print(f"[ERR] {run_id} {scale_tag}: {e!r}", flush=True)
                    rows = [{"run_id": run_id, "scale": scale_tag, "target_n": tn, "iter": 0, "status": "HARNESS_ERR", "crash_reason": repr(e)[:120]}]
                all_rows.extend(rows)
    finally:
        emu_down(p)

    cols = [
        "run_id", "scale", "target_n", "iter", "package", "status", "crash_reason",
        "launch_ms", "launch_wall_ms", "pss_mb", "rss_mb", "java_heap_mb", "vmhwm_mb",
        "frames_total", "janky_pct", "p90_ms", "frame_ms_med", "frame_ms_p90",
        "search_tested", "search_p90_ms", "search_jank_pct", "search_wall_ms",
    ]
    with open(RES_DIR / "e7a_raw.csv", "w", newline="") as f:
        w = csv.DictWriter(f, cols, extrasaction="ignore")
        w.writeheader()
        w.writerows(all_rows)
    return cmd_summarize()


def _med(xs):
    vals = []
    for x in xs:
        try:
            if x != "" and x is not None:
                vals.append(float(x))
        except ValueError:
            pass
    return round(statistics.median(vals), 1) if vals else ""


def cmd_summarize() -> int:
    raw_path = RES_DIR / "e7a_raw.csv"
    if not raw_path.exists():
        print("No e7a_raw.csv found")
        return 1
    with open(raw_path) as f:
        raw = list(csv.DictReader(f))

    summary = []
    for run_id, orig_n in APPS:
        for scale_tag, target_n in SCALES:
            sub = [r for r in raw if r["run_id"] == run_id and r["scale"] == scale_tag]
            ok = [r for r in sub if r["status"] == "OK"]
            crashes = [r for r in sub if r["status"].startswith(("CRASH", "LAUNCH_TIMEOUT"))]
            summary.append({
                "run_id": run_id,
                "scale": scale_tag,
                "target_n": orig_n if scale_tag == "orig" else target_n,
                "n_iters": len(sub),
                "n_ok": len(ok),
                "n_crash_or_timeout": len(crashes),
                "crash_reasons": ";".join(sorted({r["crash_reason"] for r in crashes if r.get("crash_reason")})),
                "launch_ms_med": _med([r["launch_ms"] for r in ok]),
                "pss_mb_med": _med([r["pss_mb"] for r in ok]),
                "rss_mb_med": _med([r["rss_mb"] for r in ok]),
                "vmhwm_mb_med": _med([r["vmhwm_mb"] for r in ok]),
                "java_heap_mb_med": _med([r["java_heap_mb"] for r in ok]),
                "janky_pct_med": _med([r["janky_pct"] for r in ok]),
                "frame_ms_p90_med": _med([r["frame_ms_p90"] for r in ok]),
                "search_tested": int(any(str(r.get("search_tested")) == "1" for r in ok)),
                "search_p90_ms_med": _med([r["search_p90_ms"] for r in ok]),
            })

    with open(RES_DIR / "e7a_summary.csv", "w", newline="") as f:
        w = csv.DictWriter(f, list(summary[0].keys()))
        w.writeheader()
        w.writerows(summary)

    print("\n=== E7a SUMMARY ===")
    for r in summary:
        print(
            f"{r['run_id']:18s} {r['scale']:7s} (N={r['target_n']:6}) ok={r['n_ok']}/{r['n_iters']} "
            f"crash={r['n_crash_or_timeout']}({r['crash_reasons']}) "
            f"launch={r['launch_ms_med']}ms pss={r['pss_mb_med']}MB rss={r['rss_mb_med']}MB "
            f"hwm={r['vmhwm_mb_med']}MB heap={r['java_heap_mb_med']}MB jank={r['janky_pct_med']}% "
            f"sp90={r['search_p90_ms_med']}ms"
        )
    return 0


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("cmd", choices=["build", "measure", "summarize"])
    ap.add_argument("--iters", type=int, default=5)
    args = ap.parse_args()
    if args.cmd == "build":
        return cmd_build()
    if args.cmd == "measure":
        return cmd_measure(args.iters)
    return cmd_summarize()


if __name__ == "__main__":
    sys.exit(main())
