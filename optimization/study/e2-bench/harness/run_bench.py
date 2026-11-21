#!/usr/bin/env python3
"""E2 benchmark harness: ABAB-interleaved A/B Macrobenchmark runs on one emulator, appended to results/raw.csv.

Run it holding both locks, with the emulator booted and nothing else building:
  flock <study-work>/gradle.lock flock <study-work>/bench.lock python3 run_bench.py [--factors P08,P13]

Resumable: a (factor, variant, round) already in raw.csv is skipped, so a restart after an emulator crash continues.
Exit codes: 0 done, 2 bad setup, 3 device lost (restart the emulator and run again).
"""
import argparse, csv, hashlib, json, os, re, subprocess, sys, time, zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
E2 = HERE.parent
REPO = E2.parents[2]
RES = Path(os.environ.get("E2_RES", E2 / "results"))
ADB = os.path.expanduser("~/Android/Sdk/platform-tools/adb")
REL, NOR8, BENCH = "shelftap.optlab", "shelftap.optlab.nor8", "shelftap.benchmark"
MEDIA = f"/sdcard/Android/media/{BENCH}"
SERIAL = os.environ.get("ANDROID_SERIAL", "emulator-5570")
DEVICE_LABEL = "lowend34"  # API 34 google_apis x86_64, 2 cores, 2 GB, 720x1280@280, swiftshader_indirect

# factor_id -> {variant: (instrumentation factor, package, compilation, gradle build type)}
FACTORS = {
    "P08": {"A": ("P08", REL, "full", "release"), "B": ("P08", REL, "full", "release")},
    "P06": {"A": ("P06", REL, "full", "release"), "B": ("P06", REL, "full", "release")},
    "P01+P09": {"A": ("P01P09", REL, "full", "release"), "B": ("P01P09", REL, "full", "release")},
    "P13": {"A": ("P13", REL, "full", "release"), "B": ("P13", REL, "full", "release")},
    "P03+P04": {"A": ("P03P04", REL, "full", "release"), "B": ("P03P04", REL, "full", "release")},
    "P10": {"A": ("startup", NOR8, "none", "noR8"), "B": ("startup", REL, "none", "release")},
    "P11": {"A": ("startup", REL, "none", "release"), "B": ("startup", REL, "profile", "release")},
}
# Scaling sweep for P01+P09: how the eager-Column cost grows with the row count (the app default for the main row is 5000).
for _n in (1000, 20000):
    FACTORS[f"P01+P09/n={_n}"] = {v: ("P01P09", REL, "full", "release", _n) for v in "AB"}
# Variants that run the same APK code with a knob changed are not slop code; the rest are provisional until E1 corpus code replaces A.
PROVISIONAL_A = {"P08", "P06", "P01+P09", "P13", "P03+P04"} | {f for f in FACTORS if f.startswith("P01+P09/")}


def sh(*args, timeout=60, check=False):
    return subprocess.run(args, capture_output=True, text=True, timeout=timeout, check=check)


def adb(*args, timeout=60):
    return sh(ADB, "-s", SERIAL, *args, timeout=timeout)


def device_ok():
    try:
        return adb("get-state", timeout=15).stdout.strip() == "device"
    except subprocess.TimeoutExpired:
        return False


def wait_device(seconds=150):
    end = time.time() + seconds
    while time.time() < end:
        if device_ok() and adb("shell", "getprop", "sys.boot_completed", timeout=15).stdout.strip() == "1":
            return True
        time.sleep(5)
    return False


def host_mem():
    m = dict(l.split(":")[0:2] for l in Path("/proc/meminfo").read_text().splitlines())
    kb = lambda k: int(m[k].split()[0]) // 1024
    return kb("MemAvailable"), kb("SwapTotal") - kb("SwapFree")


def git_commit():
    head = sh("git", "-C", str(REPO), "rev-parse", "--short", "HEAD").stdout.strip()
    dirty = sh("git", "-C", str(REPO), "status", "--porcelain", "--", "android", timeout=30).stdout.strip()
    return head + ("+wip" if dirty else "")


def unit_of(metric):
    if re.search(r"Ms(_p\d+)?$", metric) or metric.endswith("_ms"):
        return "ms"
    if metric.endswith("Kb"):
        return "KB"
    if metric.endswith("Bytes") or metric.endswith("_bytes"):
        return "bytes"
    if metric.endswith("_pct"):
        return "%"
    if metric == "frameCount":
        return "frames"
    return "count"


def pct(sorted_vals, q):
    if not sorted_vals:
        return None
    k = (len(sorted_vals) - 1) * q
    lo = int(k)
    hi = min(lo + 1, len(sorted_vals) - 1)
    return sorted_vals[lo] + (sorted_vals[hi] - sorted_vals[lo]) * (k - lo)


def frame_metrics(sampled):
    """Per-iteration frame stats from Macrobenchmark's raw per-frame samples (FrameTimingMetric)."""
    out = {}
    dur = sampled.get("frameDurationCpuMs", {}).get("runs", [])
    over = sampled.get("frameOverrunMs", {}).get("runs", [])
    for i, frames in enumerate(dur):
        s = sorted(frames)
        out.setdefault(i, {}).update({
            "frameDurationCpuMs_p50": pct(s, .50), "frameDurationCpuMs_p90": pct(s, .90), "frameDurationCpuMs_p99": pct(s, .99),
            "slowFrames_pct": 100.0 * sum(1 for f in frames if f > 16.67) / max(len(frames), 1),
        })
    for i, frames in enumerate(over):
        s = sorted(frames)
        out.setdefault(i, {}).update({"frameOverrunMs_p50": pct(s, .50), "frameOverrunMs_p90": pct(s, .90), "frameOverrunMs_p99": pct(s, .99)})
    return out


def run_block(factor_id, variant, spec, iters, first_iteration, keep_trace_as):
    inst_factor, pkg, comp, _bt = spec[:4]
    rows = spec[4] if len(spec) > 4 else None
    cmd = [
        "shell", "am", "instrument", "-w", "-r", "-e", "class", f"{BENCH}.FactorBenchmark#run",
        "-e", "androidx.benchmark.suppressErrors", "EMULATOR,LOW-BATTERY",
        "-e", "st.factor", inst_factor, "-e", "st.variant", variant if inst_factor != "startup" else "B",
        "-e", "st.target", pkg, "-e", "st.compilation", comp, "-e", "st.iters", str(iters),
    ]
    if rows:
        cmd += ["-e", "st.n", str(rows)]
    cmd.append(f"{BENCH}/androidx.test.runner.AndroidJUnitRunner")
    adb("shell", f"rm -f {MEDIA}/*")
    t0 = time.time()
    try:
        r = adb(*cmd, timeout=900)
    except subprocess.TimeoutExpired:
        return None, time.time() - t0, "timeout"
    secs = time.time() - t0
    if "OK (1 test)" not in r.stdout:
        return None, secs, (re.findall(r"stack=(.*)", r.stdout) or [r.stdout[-300:]])[0][:200]
    jp = RES / "_tmp.json"
    adb("pull", f"{MEDIA}/{BENCH}-benchmarkData.json", str(jp))
    data = json.loads(jp.read_text())["benchmarks"][0]
    jp.unlink()
    if keep_trace_as:
        tr = adb("shell", f"ls {MEDIA}/*.perfetto-trace").stdout.split()
        if tr:
            dst = Path(os.environ.get("TRACE_DIR", "/mnt/01DC198BE9192500/github/Schooldev/3S-study-work/e2-traces"))
            dst.mkdir(parents=True, exist_ok=True)
            adb("pull", tr[0], str(dst / f"{keep_trace_as}.perfetto-trace"), timeout=120)
    adb("shell", f"rm -f {MEDIA}/*")
    per_iter = {}
    for name, m in data["metrics"].items():
        for i, v in enumerate(m["runs"]):
            per_iter.setdefault(i, {})[name] = v
    for i, d in frame_metrics(data.get("sampledMetrics", {})).items():
        per_iter.setdefault(i, {}).update(d)
    return per_iter, secs, "ok"


def apk_sizes():
    """Static device-independent size metrics for P10, straight from the built APKs."""
    out = {}
    for variant, bt in (("A", "noR8"), ("B", "release")):
        apk = next((REPO / "android/optlab/build/outputs/apk" / bt).glob("*.apk"))
        with zipfile.ZipFile(apk) as z:
            infos = z.infolist()
            dex = sum(i.file_size for i in infos if re.fullmatch(r"classes\d*\.dex", i.filename))
            res = sum(i.file_size for i in infos if i.filename.startswith("res/") or i.filename == "resources.arsc")
        out[variant] = {"apk_bytes": apk.stat().st_size, "dex_bytes": dex, "resources_bytes": res}
    return out


def environment(args):
    toml = (REPO / "android/gradle/libs.versions.toml").read_text()
    ver = lambda k: re.search(rf'^{k} = "([^"]+)"', toml, re.M).group(1)
    apks = {}
    for label, p in (("optlab-release", "android/optlab/build/outputs/apk/release"), ("optlab-noR8", "android/optlab/build/outputs/apk/noR8"),
                     ("benchmark-release", "android/benchmark/build/outputs/apk/release")):
        f = next((REPO / p).glob("*.apk"))
        apks[label] = {"file": f.name, "bytes": f.stat().st_size, "sha256": hashlib.sha256(f.read_bytes()).hexdigest()}
    prop = lambda k: adb("shell", "getprop", k).stdout.strip()
    avail, swap = host_mem()
    return {
        "started": time.strftime("%Y-%m-%dT%H:%M:%S%z"), "commit": git_commit(), "device": DEVICE_LABEL, "serial": SERIAL,
        "android_sdk": prop("ro.build.version.sdk"), "model": prop("ro.product.model"), "fingerprint": prop("ro.build.fingerprint"),
        "display": adb("shell", "wm size").stdout.strip() + " / " + adb("shell", "wm density").stdout.strip(),
        "guest_cores": adb("shell", "nproc").stdout.strip(), "guest_mem_kb": adb("shell", "grep MemTotal /proc/meminfo").stdout.split()[1],
        "gpu_mode": "swiftshader_indirect (software), headless -no-window", "emulator_flags": "-no-snapshot -no-boot-anim -no-audio -memory 2048 -cores 2",
        "versions": {k: ver(k) for k in ("agp", "kotlin", "composeBom", "coil", "benchmark", "uiautomator", "profileinstaller", "coroutines")},
        "apks": apks, "blocks": args.blocks, "block_iters": args.block_iters, "ordering": "rounds alternate A,B / B,A",
        "host_mem_available_mb_at_start": avail, "host_swap_used_mb_at_start": swap,
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--factors", default=",".join(f for f in FACTORS if "/" not in f), help="default: the seven main factors; add P01+P09/n=1000,P01+P09/n=20000 for the sweep")
    ap.add_argument("--blocks", type=int, default=4, help="rounds; each round runs A then B (or B then A)")
    ap.add_argument("--block-iters", type=int, default=3, help="Macrobenchmark iterations per am-instrument call")
    args = ap.parse_args()
    RES.mkdir(parents=True, exist_ok=True)
    if not wait_device(30):
        print("device not available", file=sys.stderr)
        return 3
    raw, log = RES / "raw.csv", RES / "runlog.csv"
    done, sizes_done = set(), False
    if raw.exists():
        for r in csv.DictReader(raw.open()):
            done.add((r["factor_id"], r["variant"], int(r["iteration"])))
            sizes_done |= r["metric"] == "apk_bytes"
    new = not raw.exists()
    rf = raw.open("a", newline="")
    w = csv.writer(rf, lineterminator="\n")
    if new:
        w.writerow(["factor_id", "variant", "metric", "unit", "iteration", "value", "device", "build_type", "commit"])
    lf = log.open("a", newline="")
    lw = csv.writer(lf, lineterminator="\n")
    if log.stat().st_size == 0:
        lw.writerow(["time", "factor_id", "variant", "round", "seconds", "result", "host_mem_available_mb", "host_swap_used_mb"])
    env_path = RES / "environment.json"
    if not env_path.exists():
        env_path.write_text(json.dumps(environment(args), indent=2))
    commit = git_commit()

    for fid in args.factors.split(","):
        specs = FACTORS[fid]
        for rnd in range(args.blocks):
            order = ("A", "B") if rnd % 2 == 0 else ("B", "A")
            for variant in order:
                first = rnd * args.block_iters + 1
                if (fid, variant, first) in done:
                    continue
                spec = specs[variant]
                build_type = spec[3] + (";A-provisional" if variant == "A" and fid in PROVISIONAL_A else "")
                for attempt in (1, 2):
                    if not device_ok() and not wait_device(150):
                        print("device lost", file=sys.stderr)
                        return 3
                    per_iter, secs, status = run_block(fid, variant, spec, args.block_iters, first,
                                                       f"{fid}-{variant}-r{rnd}" if rnd == 0 else None)
                    avail, swap = host_mem()
                    lw.writerow([time.strftime("%H:%M:%S"), fid, variant, rnd, f"{secs:.0f}", status, avail, swap])
                    lf.flush()
                    if per_iter is not None:
                        break
                    print(f"{fid} {variant} round {rnd} attempt {attempt}: {status}", file=sys.stderr)
                else:
                    print(f"giving up on {fid} {variant} round {rnd}", file=sys.stderr)
                    continue
                for i, metrics in per_iter.items():
                    for metric, value in metrics.items():
                        w.writerow([fid, variant, metric, unit_of(metric), first + i, f"{value:.6g}", DEVICE_LABEL, build_type, commit])
                rf.flush()
                print(f"{fid} {variant} round {rnd}: {len(per_iter)} iterations in {secs:.0f}s (host avail {avail} MB)", flush=True)
        if fid == "P10" and not sizes_done:
            sizes_done = True
            for variant, vals in apk_sizes().items():
                for metric, value in vals.items():
                    w.writerow(["P10", variant, metric, unit_of(metric), 1, value, DEVICE_LABEL, "noR8" if variant == "A" else "release", commit])
            rf.flush()
    return 0


if __name__ == "__main__":
    sys.exit(main())
