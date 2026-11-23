#!/usr/bin/env python3
"""E2 benchmark harness: ABAB-interleaved A/B Macrobenchmark runs on one emulator, appended to results/raw.csv.

Run it with the emulator booted and nothing else building (no gradle at the same time as the emulator):
  python3 run_bench.py [--factors P08,P13] [--phase primary,frames]
It takes the EXCLUSIVE bench lock (study-work/bench.lock) around every measured block (one am-instrument call), so other panes
that hold it shared during AI generation pause while a block runs and continue between blocks.

Phases (protocol: counts first, frame metrics last): "primary" = counters, trace slices, memory, startup timing; "frames" = FrameTimingMetric
only, main factors only. Each phase is a separate run of the same ABAB design.

Host conditions are sampled at the start of every measured iteration (the app logs tag STBENCH iter-begin / iter-end): MemAvailable (MB),
1-minute load average, and pswpin (swap-in pages) between begin and end. Pre-declared exclusion: an iteration is dropped when
mem_avail_mb < 1024 or any swap-in happened during it. Dropped iterations are kept in iterlog.csv (status column), not in raw.csv;
a (factor, variant, phase) short of its target gets extra rounds (--max-extra, default 3).

Resumable: a block already marked ok in runlog.csv is skipped, so a restart after an emulator crash continues.
Exit codes: 0 done, 2 bad setup, 3 device lost (restart the emulator and run again).
"""
import argparse, atexit, csv, fcntl, signal, hashlib, json, os, re, subprocess, sys, threading, time, zipfile
from collections import defaultdict
from pathlib import Path

HERE = Path(__file__).resolve().parent
E2 = HERE.parent
REPO = E2.parents[2]
RES = Path(os.environ.get("E2_RES", E2 / "results"))
ADB = os.path.expanduser("~/Android/Sdk/platform-tools/adb")
REL, NOR8, BENCH = "shelftap.optlab", "shelftap.optlab.nor8", "shelftap.benchmark"
MEDIA = f"/sdcard/Android/media/{BENCH}"
SERIAL = os.environ.get("ANDROID_SERIAL", "emulator-5570")
STUDY_WORK = os.environ.get("STUDY_WORK", "/mnt/01DC198BE9192500/github/Schooldev/3S-study-work")
BENCH_LOCK = os.path.join(STUDY_WORK, "bench.lock")
TURNSTILE = os.path.join(STUDY_WORK, "bench.turnstile")  # writer priority, see BenchLock (legacy path, --turnstile)
BENCH_HOLD = os.path.join(STUDY_WORK, "bench.hold")  # holds our pid while we queue for or hold bench.lock; generators wait on it before they take the turnstile (alpha, RE#dl-1)
WINDOW_S, COOLDOWN_S = 10 * 60, 120  # turnstile held at most WINDOW_S while waiting for the lock, then released for COOLDOWN_S so peers drain
PATIENT = True  # default: never touch the turnstile, write bench.hold and poll the lock; --turnstile restores the old turnstile windows
YIELD_S = 120  # after every factor group (phase, factor): lock and turnstile released, then this long before the next acquire (REQ#yield)
MAX_HOLD_S = 18 * 60  # alpha: hold the bench lock per factor, at most about 20 minutes
MIN_AVAIL_MB = 1024  # pre-declared exclusion threshold (host MemAvailable at iteration start)
QUERY = "silent river"  # the P13 typed query; keep equal to benchmark/Journeys.kt QUERY
DEVICE_LABEL = "lowend34"  # API 34 google_apis x86_64, 2 cores, 2 GB, 720x1280@280, swiftshader_indirect

# factor_id -> {variant: (instrumentation factor, package, compilation, gradle build type[, rows])}
# P08, P06, P01+P09, P13, P03+P04: A is corpus code (optlab corpus/*, ports/README.md); "<id>/author" keeps the author-constructed pilot pair for sensitivity.
FACTORS = {
    "P08": {v: ("P08c", REL, "full", "release") for v in "AB"},
    "P06": {v: ("P06c", REL, "full", "release") for v in "AB"},
    "P01+P09": {v: ("P09c", REL, "full", "release") for v in "AB"},       # P09 part of the corpus pair: eager sheet vs lazy sheet
    "P13": {v: ("P13c", REL, "full", "release") for v in "AB"},
    "P03+P04": {v: ("P03P04c", REL, "full", "release") for v in "AB"},
    "P10": {"A": ("startup", NOR8, "none", "noR8"), "B": ("startup", REL, "none", "release")},
    "P11": {"A": ("startup", REL, "none", "release"), "B": ("startup", REL, "profile", "release")},
    "P01+P09/P01": {v: ("P01c", REL, "full", "release") for v in "AB"},   # P01 part of the corpus pair: list items without key vs with key
    "P08/author": {v: ("P08", REL, "full", "release") for v in "AB"},
    "P06/author": {v: ("P06", REL, "full", "release") for v in "AB"},
    "P01+P09/author": {v: ("P01P09", REL, "full", "release") for v in "AB"},
    "P13/author": {v: ("P13", REL, "full", "release") for v in "AB"},
    "P03+P04/author": {v: ("P03P04", REL, "full", "release") for v in "AB"},
}
# Scaling sweep for P01+P09 (corpus sheet): how the eager-Column cost grows with the number of loans (the main row uses 2000).
for _n in (500, 8000):
    FACTORS[f"P01+P09/n={_n}"] = {v: ("P09c", REL, "full", "release", _n) for v in "AB"}
MAIN = ["P08", "P06", "P01+P09", "P13", "P03+P04", "P10", "P11"]
# Variants that run the same APK code with a knob changed are not slop code. A is corpus code for the rest, so nothing is provisional once the ports are in.
PROVISIONAL_A = {f for f in FACTORS if f.endswith("/author")}


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


def host_sample():
    """(MemAvailable MB, 1-minute load average, pswpin pages since boot) of the host, read from /proc."""
    mem = next(int(l.split()[1]) for l in Path("/proc/meminfo").read_text().splitlines() if l.startswith("MemAvailable:")) // 1024
    load = float(Path("/proc/loadavg").read_text().split()[0])
    swapin = next(int(l.split()[1]) for l in Path("/proc/vmstat").read_text().splitlines() if l.startswith("pswpin "))
    return mem, load, swapin


_QEMU = {}


def emu_sample():
    """(major faults, swapped-out KB) of the emulator's qemu process; the faults that can slow the guest are its own swap-ins."""
    if "pid" not in _QEMU or not Path(f"/proc/{_QEMU['pid']}").exists():
        out = sh("pgrep", "-f", f"qemu-system.*-port {SERIAL.split('-')[-1]}").stdout.split()
        _QEMU["pid"] = out[0] if out else None
    pid = _QEMU["pid"]
    if not pid:
        return None
    try:
        majflt = int(Path(f"/proc/{pid}/stat").read_text().rsplit(")", 1)[1].split()[9])
        swap = next(int(l.split()[1]) for l in Path(f"/proc/{pid}/status").read_text().splitlines() if l.startswith("VmSwap:"))
    except (OSError, StopIteration, ValueError, IndexError):
        return None
    return majflt, swap


def hold_on():
    Path(BENCH_HOLD).write_text(str(os.getpid()))


def hold_off():
    try:
        if Path(BENCH_HOLD).read_text().strip() == str(os.getpid()):
            os.remove(BENCH_HOLD)
    except OSError:
        pass


atexit.register(hold_off)
signal.signal(signal.SIGTERM, lambda *_: sys.exit(143))  # runs atexit, so a killed run never leaves bench.hold behind


class BenchLock:
    """Exclusive flock on study-work/bench.lock while the emulator is measuring; other panes hold it shared around AI generation.

    Held per (phase, factor): taken before the first block, kept across that factor's blocks, released when the factor ends
    (or after MAX_HOLD_S at a block boundary), so peers run in the gaps. Never held for setup, builds or debugging.

    flock has no writer priority: peers that re-take the shared lock before the previous holder lets go starve us. So we hold
    bench.turnstile (exclusive) in short windows while polling for the lock; a peer that takes the turnstile around its shared lock
    waits behind us, which lets the current shared holders drain. Windows are bounded and followed by a cooldown, because a peer
    that holds the shared lock and then needs the turnstile (nested acquire) would otherwise deadlock against us (seen 2026-11-21 09:00-09:32).
    """

    def __init__(self):
        self.f, self.since, self.waited = None, 0.0, 0.0

    def acquire(self):
        self.waited = 0.0
        if self.f:
            return self
        t0 = time.time()
        f = open(BENCH_LOCK, "a")
        while PATIENT:
            # bench.hold exists for the whole window (queueing, then holding); a window that gets nothing removes it and gives the generators COOLDOWN_S
            hold_on()
            end = time.time() + WINDOW_S
            while time.time() < end:
                try:
                    fcntl.flock(f, fcntl.LOCK_EX | fcntl.LOCK_NB)
                    self.f, self.since = f, time.time()
                    self.waited = self.since - t0
                    return self
                except BlockingIOError:
                    time.sleep(0.2)
            hold_off()
            time.sleep(COOLDOWN_S)
        while True:
            # Hold the turnstile for a bounded window only, polling the lock without blocking. A peer that already holds the shared lock
            # and then waits for the turnstile (nested acquire) would deadlock against a blocking wait; between windows it gets through.
            turnstile = open(TURNSTILE, "a")
            fcntl.flock(turnstile, fcntl.LOCK_EX)
            got, end = False, time.time() + WINDOW_S
            try:
                while time.time() < end:
                    try:
                        fcntl.flock(f, fcntl.LOCK_EX | fcntl.LOCK_NB)
                        got = True
                        break
                    except BlockingIOError:
                        time.sleep(0.5)
            finally:
                fcntl.flock(turnstile, fcntl.LOCK_UN)
                turnstile.close()
            if got:
                break
            time.sleep(COOLDOWN_S)
        self.f, self.since = f, time.time()
        self.waited = self.since - t0
        return self

    def release(self):
        if self.f:
            fcntl.flock(self.f, fcntl.LOCK_UN)
            self.f.close()
            self.f = None
        hold_off()

    def release_if_long(self):
        if self.f and time.time() - self.since > MAX_HOLD_S:
            self.release()

    def __enter__(self):
        return self.acquire()

    def __exit__(self, *exc):
        self.release()


class IterWatch:
    """Follows logcat tag STBENCH during one am-instrument call; samples the host at every iter-begin and iter-end."""

    def __init__(self):
        self.iters, self._cur = [], None

    def start(self):
        adb("logcat", "-c")
        self.p = subprocess.Popen([ADB, "-s", SERIAL, "logcat", "-v", "brief", "-s", "STBENCH:I"], stdout=subprocess.PIPE,
                                  stderr=subprocess.DEVNULL, text=True, bufsize=1)
        self.t = threading.Thread(target=self._read, daemon=True)
        self.t.start()
        time.sleep(1.5)

    def _read(self):
        for line in self.p.stdout:
            if "iter-begin" in line:
                self._cur = (host_sample(), emu_sample())
            elif "iter-end" in line and self._cur:
                (mem, load, s0), e0 = self._cur
                e1 = emu_sample()
                self.iters.append({"mem": mem, "load": load, "swapin": host_sample()[2] - s0,
                                   "emu_majflt": (e1[0] - e0[0]) if e0 and e1 else None, "emu_swap_kb": (e1[1] - e0[1]) if e0 and e1 else None})
                self._cur = None

    def stop(self):
        time.sleep(1)
        self.p.terminate()
        self.t.join(5)


def wait_quiet(max_s=180):
    """Before a block: wait (bounded) until the host has some headroom, so the block is not dropped for memory."""
    end = time.time() + max_s
    while host_sample()[0] < MIN_AVAIL_MB + 512 and time.time() < end:
        time.sleep(10)


def exclusion(sample, rule):
    """'' = keep, else the reason. mem: host MemAvailable < 1024 MB at start. swapin: rule "system" = any host pswpin during the
    iteration (protocol A2 as written), rule "emulator" = any major fault of the emulator process (its own swap-ins)."""
    if sample is None:
        return "no-sample"
    why = []
    if sample["mem"] < MIN_AVAIL_MB:
        why.append("mem<1024")
    swapped = sample["swapin"] > 0 if rule == "system" else (sample["emu_majflt"] is None or sample["emu_majflt"] > 0)
    if swapped:
        why.append("swapin")
    return "+".join(why)


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


def run_block(factor_id, variant, spec, iters, phase, keep_trace_as):
    inst_factor, pkg, comp, _bt = spec[:4]
    rows = spec[4] if len(spec) > 4 else None
    cmd = [
        "shell", "am", "instrument", "-w", "-r", "-e", "class", f"{BENCH}.FactorBenchmark#run",
        "-e", "androidx.benchmark.suppressErrors", "EMULATOR,LOW-BATTERY",
        "-e", "st.factor", inst_factor, "-e", "st.variant", variant if inst_factor != "startup" else "B",
        "-e", "st.target", pkg, "-e", "st.compilation", comp, "-e", "st.iters", str(iters), "-e", "st.metrics", phase,
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
    if keep_trace_as and os.environ.get("TRACE_DIR"):  # traces are kept only when asked for (file-count hygiene); the REPORT cites none
        tr = adb("shell", f"ls {MEDIA}/*.perfetto-trace").stdout.split()
        if tr:
            dst = Path(os.environ["TRACE_DIR"])
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
    """Static device-independent size metrics for P10, straight from the built APKs (copies in study-work/apk, so build/ can be cleaned)."""
    out = {}
    for variant, name in (("A", "optlab-noR8.apk"), ("B", "optlab-release.apk")):
        apk = Path(STUDY_WORK) / "apk" / name
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
    for label, name in (("optlab-release", "optlab-release.apk"), ("optlab-noR8", "optlab-noR8.apk"), ("benchmark-release", "benchmark-release.apk")):
        f = Path(STUDY_WORK) / "apk" / name
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
        "phases": "primary (counters, slices, memory, startup) first, frames (FrameTimingMetric) last",
        "bench_lock": "exclusive flock per measured block (one am-instrument call)",
        "exclusion_rule": f"iteration dropped if host MemAvailable < {MIN_AVAIL_MB} MB at iteration start or swap-in during it (criterion '{args.swap_rule}': 'system' = host pswpin > 0, 'emulator' = qemu process major faults > 0; both logged in iterlog.csv); extra rounds up to --max-extra",
        "bootstrap_seed": 20261121,
    }


def p13_equivalence(res):
    """One-off semantic check for P13: A and B result sets per typed prefix (see optlab EquivScreen). Writes p13_equivalence.csv."""
    out = res / "p13_equivalence.csv"
    if out.exists():
        return True
    with BenchLock():
        adb("logcat", "-c")
        adb("shell", f"am start -W -n {REL}/shelftap.optlab.MainActivity --es factor p13eq --es query '{QUERY}'")  # one string: the device shell splits words
        end, txt = time.time() + 900, ""
        while time.time() < end:
            txt = adb("logcat", "-d", "-s", "STLAB:I").stdout
            if f"p13eq i={len(QUERY)} " in txt:
                break
            time.sleep(3)
        adb("shell", "am", "force-stop", REL)
    rows = re.findall(r'p13eq i=(\d+) q="([^"]*)" a=(\d+) h=(\d+) b=(\d+) bpage=(\d+) jac_full=(\S+) jac_page=(\S+) jac_h_full=(\S+) jac_h_page=(\S+)', txt)
    if len(rows) != len(QUERY):
        print(f"P13 equivalence run incomplete ({len(rows)} of {len(QUERY)} prefixes)", file=sys.stderr)
        return False
    with out.open("w", newline="") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["i", "query", "a_corpus_like_count", "h_token_prefix_count", "b_fts_count_uncapped", "b_fts_count_page120", "jaccard_a_vs_b_all", "jaccard_a_page120_vs_b_page120", "jaccard_h_vs_b_all", "jaccard_h_page120_vs_b_page120"])
        w.writerows(rows)
    return True


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--factors", default=",".join(MAIN), help="default: the seven main factors; add P01+P09/n=1000,P01+P09/n=20000 for the sweep (primary phase only)")
    ap.add_argument("--phase", default="primary,frames", help="comma list, run in this order")
    ap.add_argument("--blocks", type=int, default=4, help="rounds; each round runs A then B (or B then A)")
    ap.add_argument("--block-iters", type=int, default=3, help="Macrobenchmark iterations per am-instrument call")
    ap.add_argument("--swap-rule", default="emulator", choices=["emulator", "system"], help="swap-in criterion of the exclusion rule: the emulator process's major faults, or any host pswpin (see exclusion())")
    ap.add_argument("--lock-window", type=float, default=10, help="minutes the turnstile is held per attempt while waiting for the lock (use more than the longest peer generation, 15 min)")
    ap.add_argument("--turnstile", action="store_true", help="legacy: wait for bench.lock in turnstile windows instead of bench.hold + polling")
    ap.add_argument("--max-extra", type=int, default=3, help="extra rounds per (factor, phase) when exclusions leave a variant short of blocks*block-iters")
    args = ap.parse_args()
    global PATIENT, WINDOW_S
    PATIENT = not args.turnstile
    WINDOW_S = int(args.lock_window * 60)
    RES.mkdir(parents=True, exist_ok=True)
    if not wait_device(30):
        print("device not available", file=sys.stderr)
        return 3
    raw, log, itl = RES / "raw.csv", RES / "runlog.csv", RES / "iterlog.csv"
    target = args.blocks * args.block_iters
    done, kept, sizes_done = set(), defaultdict(int), False
    if log.exists():
        for r in csv.DictReader(log.open()):
            if r["result"] == "ok":
                done.add((r["factor_id"], r["phase"], r["variant"], int(r["round"])))
    if itl.exists():
        for r in csv.DictReader(itl.open()):
            kept[(r["factor_id"], r["phase"], r["variant"])] += r["status"] == "kept"
    if raw.exists():
        sizes_done = any(r["metric"] == "apk_bytes" for r in csv.DictReader(raw.open()))
    new = not raw.exists()
    rf = raw.open("a", newline="")
    w = csv.writer(rf, lineterminator="\n")
    RAW_HEAD = ["factor_id", "variant", "metric", "unit", "iteration", "value", "device", "build_type", "commit", "mem_avail_mb", "loadavg1"]
    if new:
        w.writerow(RAW_HEAD)
    # sensitivity analysis (alpha RE#q4): every measured iteration, kept or dropped, with its status; raw.csv is the primary analysis (kept only)
    raw_all = RES / "raw_all.csv"
    new_all = not raw_all.exists()
    af = raw_all.open("a", newline="")
    aw = csv.writer(af, lineterminator="\n")
    if new_all:
        aw.writerow(RAW_HEAD + ["status"])
    lf, tf = log.open("a", newline=""), itl.open("a", newline="")
    lw, tw = csv.writer(lf, lineterminator="\n"), csv.writer(tf, lineterminator="\n")
    if log.stat().st_size == 0:
        lw.writerow(["time", "factor_id", "phase", "variant", "round", "seconds", "result", "host_mem_available_mb", "host_swap_used_mb", "iters_kept", "iters_dropped", "lock_wait_s"])
    if itl.stat().st_size == 0:
        tw.writerow(["time", "factor_id", "phase", "variant", "iteration", "mem_avail_mb", "loadavg1", "swapin_pages", "emu_majflt", "emu_swap_delta_kb", "status"])
    env_path = RES / "environment.json"
    if not env_path.exists():
        env_path.write_text(json.dumps(environment(args), indent=2))
    commit = git_commit()
    if not p13_equivalence(RES):
        return 2

    lock = BenchLock()
    lock_taken = [False]  # a factor group got the lock at least once (only then is there something to yield)

    def block(fid, phase, rnd, variant):
        spec = FACTORS[fid][variant]
        first = rnd * args.block_iters + 1
        build_type = spec[3] + (";A-provisional" if variant == "A" and fid in PROVISIONAL_A else "")
        for attempt in (1, 2):
            lock.release_if_long()
            lock.acquire()
            lock_taken[0] = True
            if not device_ok() and not wait_device(150):
                print("device lost", file=sys.stderr)
                return 3
            wait_quiet()
            watch = IterWatch()
            watch.start()
            try:
                per_iter, secs, status = run_block(fid, variant, spec, args.block_iters, phase,
                                                   f"{fid}-{variant}-r{rnd}" if rnd == 0 and phase == "primary" else None)
            finally:
                watch.stop()
            avail, swap = host_mem()
            n_kept = n_drop = 0
            if per_iter is not None:
                samples = watch.iters[-len(per_iter):] if len(watch.iters) >= len(per_iter) else watch.iters + [None] * (len(per_iter) - len(watch.iters))
                for i, metrics in per_iter.items():
                    smp, why = samples[i], exclusion(samples[i], args.swap_rule)
                    tw.writerow([time.strftime("%H:%M:%S"), fid, phase, variant, first + i, smp["mem"] if smp else "", smp["load"] if smp else "",
                                 smp["swapin"] if smp else "", smp["emu_majflt"] if smp else "", smp["emu_swap_kb"] if smp else "",
                                 "kept" if not why else "dropped:" + why])
                    for metric, value in metrics.items():
                        row = [fid, variant, metric, unit_of(metric), first + i, f"{value:.6g}", DEVICE_LABEL, build_type, commit,
                               smp["mem"] if smp else "", smp["load"] if smp else ""]
                        aw.writerow(row + ["kept" if not why else "dropped:" + why])
                        if not why:
                            w.writerow(row)
                    if why:
                        n_drop += 1
                    else:
                        n_kept += 1
                rf.flush(); af.flush(); tf.flush()
            lw.writerow([time.strftime("%H:%M:%S"), fid, phase, variant, rnd, f"{secs:.0f}", status, avail, swap, n_kept, n_drop, f"{lock.waited:.0f}"])
            lf.flush()
            if per_iter is not None:
                kept[(fid, phase, variant)] += n_kept
                done.add((fid, phase, variant, rnd))
                print(f"{phase} {fid} {variant} round {rnd}: kept {n_kept}, dropped {n_drop} in {secs:.0f}s (host avail {avail} MB, lock wait {lock.waited:.0f}s)", flush=True)
                return 0
            print(f"{fid} {variant} round {rnd} attempt {attempt}: {status}", file=sys.stderr)
        print(f"giving up on {fid} {variant} round {rnd}", file=sys.stderr)
        return 0

    for phase in args.phase.split(","):
        for fid in args.factors.split(","):
            if phase == "frames" and "/" in fid:
                continue
            rnd = 0
            while True:
                short = min(kept[(fid, phase, "A")], kept[(fid, phase, "B")])
                if rnd >= args.blocks and (rnd - args.blocks >= args.max_extra or short >= target or short == 0):  # short == 0: a variant that never runs gets no top-up
                    break
                for variant in (("A", "B") if rnd % 2 == 0 else ("B", "A")):
                    if (fid, phase, variant, rnd) not in done and block(fid, phase, rnd, variant) == 3:
                        lock.release()
                        return 3
                rnd += 1
            lock.release()  # between factors the emulator is idle: let peers generate
            if lock_taken[0]:
                lock_taken[0] = False
                lw.writerow([time.strftime("%H:%M:%S"), fid, phase, "", "", YIELD_S, "yield", "", "", "", "", ""])
                lf.flush()
                time.sleep(YIELD_S)
            if phase == "primary" and fid == "P10" and not sizes_done:
                sizes_done = True
                avail, load, _ = host_sample()
                for variant, vals in apk_sizes().items():
                    for metric, value in vals.items():
                        w.writerow(["P10", variant, metric, unit_of(metric), 1, value, DEVICE_LABEL, "noR8" if variant == "A" else "release", commit, avail, load])
                rf.flush()
    return 0


if __name__ == "__main__":
    sys.exit(main())
