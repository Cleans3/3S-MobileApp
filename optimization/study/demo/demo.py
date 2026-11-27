#!/usr/bin/env python3
"""ShelfTap optimization demo: the AI draft (A) next to the tuned version (B) on Android emulators.

Python 3.8+ standard library only; works on Windows, Linux and macOS (x86_64 hosts).
  python demo.py doctor            check SDK, system image, acceleration, JDK 21, RAM; says one or two emulators
  python demo.py setup             build the lab app, create the AVDs, boot, install, seed the data
  python demo.py start [--single|--pair] [--gpu MODE]   boot the emulators (setup does this too)
  python demo.py list              scenarios, the slide each belongs to, the fallback clip
  python demo.py run <scenario>    same gesture on A and B, then the live numbers next to the study's
  python demo.py record <scenario> screenrecord A and B (side-by-side file if ffmpeg is on PATH)
  python demo.py stop              shut the demo emulators down
The study's numbers (optimization/study/e2-bench) are the reference; a live run is an illustration.
"""
import argparse
import json
import os
import re
import shutil
import statistics
import subprocess
import sys
import threading
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
ANDROID = REPO / "android"
OUT = HERE / "out"  # git-ignored: APK copies, logs, recordings
WIN = os.name == "nt"
PKG, PKG_NOR8 = "shelftap.optlab", "shelftap.optlab.nor8"
ACT = "shelftap.optlab.MainActivity"
AVDS = ("shelftap-demo-a", "shelftap-demo-b")
IMAGE = "system-images;android-34;google_apis;x86_64"
# The console port encodes the GPU mode, so a running emulator tells which renderer it has without any state file.
PORTS = {"swiftshader_indirect": (5580, 5582), "swangle_indirect": (5590, 5592)}
PAIR_MIN_MB = 7000  # two emulators take about 3 GB of host RAM each (2 GB guest + qemu + software GPU), plus a margin
APKS = {"release": "optlab/build/outputs/apk/release/optlab-release.apk", "noR8": "optlab/build/outputs/apk/noR8/optlab-noR8.apk"}

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace", line_buffering=True)
except Exception:
    pass

SWIPES = "for i in 1 2 3 4 5 6; do input swipe 360 1000 360 350 250; sleep 0.8; done"
TYPE_QUERY = "input tap 360 190; sleep 1; for c in s i l e n t %s r i v e r; do input text $c; sleep 0.3; done; sleep 3"  # > the 200 ms debounce: one query per keystroke

# Study values (A -> B) are copied from optimization/study/e2-bench/results/summary.csv (medians of 12+ kept iterations, AVD lowend34).
# Live metric keys: ttid, janky, p90, heap, rss, rows, decoded, qmax, qsum, apk
SCENARIOS = {
    "p09": dict(
        title="Danh sách dài dựng toàn bộ (P09): 2000 lượt mượn trong Column thay vì LazyColumn",
        slides="S7, S11", factor="P09c", gesture=SWIPES, clip="P09c-AB.mp4",
        live=["rows", "ttid", "janky", "p90", "heap", "rss"],
        study={"rows": "2000 → 31", "ttid": "1901 → 573 ms", "janky": "90% → 1,3%", "p90": "27,1 → 5,9 ms",
               "heap": "220 MB → 7,6 MB", "rss": "243 MB → 43 MB"},
        say="A dựng cả 2000 mục ngay khi mở; B chỉ dựng phần đang hiện trên màn hình."),
    "p08": dict(
        title="Hiệu ứng trang trí trên từng thẻ sách (P08): bóng 5 dp, gradient, vẽ canvas",
        slides="S8", factor="P08c", gesture=SWIPES + "; sleep 3", gpu="swangle_indirect", clip="P08c-AB.mp4",
        live=["janky", "p90", "rss"],
        study={"janky": "70,52% → 48,65%", "p90": "62,87 → 49,07 ms", "rss": "45 MB → 42 MB"},
        say="Cùng lưới bìa sách; B bỏ hiệu ứng trang trí. B vẫn chưa đạt 60 fps trên máy ảo."),
    "p06": dict(
        title="Ảnh bìa giải mã ở kích thước gốc 1500x2250 (P06) thay vì đúng ô hiển thị",
        slides="S10", factor="P06c", gesture=SWIPES, clip="P06c-AB.mp4",
        live=["decoded", "rss"],
        study={"decoded": "203 MB → 6,2 MB (32 lần)", "rss": "180 MB → 60 MB"},
        say="Cùng 15 ảnh bìa; A giải mã từng ảnh ở kích thước gốc, B giải mã vừa ô 150x210 dp."),
    "p13": dict(
        title="Tìm kiếm trên 100.000 đầu sách không có chỉ mục (P13) thay vì chỉ mục FTS4",
        slides="S12", factor="P13c", gesture=TYPE_QUERY, clip="P13c-AB.mp4",
        live=["qmax", "qsum"],
        study={"qmax": "96,63 → 37 ms (mục tiêu < 50 ms)", "qsum": "634,9 → 85,42 ms"},
        say="Gõ cùng một chuỗi 'silent river'; mỗi phím là một truy vấn."),
    "p10": dict(
        title="Bản build không bật R8 (P10): cùng mã, không thu gọn",
        slides="S12", factor="startup", gesture="", clip=None,
        live=["apk", "ttid"],
        study={"apk": "24,7 MB → 2,4 MB", "ttid": "505,6 → 461,6 ms (-8,7%)"},
        say="Cùng mã nguồn; A tắt R8 và thu gọn tài nguyên, B bật."),
    "p01": dict(
        title="Kết quả không đo được (P01): mục danh sách không có key",
        slides="S9", factor="P01c", gesture=SWIPES, clip="P01c-AB.mp4",
        live=["rows", "ttid", "janky", "p90"],
        study={"rows": "28 → 28", "ttid": "617,1 → 620 ms (không khác)", "janky": "không đo khung hình cho P01", "p90": "—"},
        say="Hai bên giống nhau: thiếu key ở đây không tốn chi phí đo được."),
    "p0304": dict(
        title="Kết quả không đo được (P03+P04): tham số không ổn định, tính toán trong composition",
        slides="S9", factor="P03P04c", gesture="input swipe 360 1000 360 350 250; sleep 0.8; input swipe 360 1000 360 350 250; sleep 5",
        clip=None, live=["recomp", "p90"],
        study={"recomp": "36,21 → 36,75 ms (không khác)", "p90": "11,56 → 9,80 ms (khoảng tin cậy chứa 0)"},
        say="Màn hình cập nhật 10 lần mỗi giây; hai bên như nhau."),
}
LABELS = {
    "rows": "số mục đã dựng (st.rc.row)",
    "ttid": "thời gian khởi động (TTID, am start -W)",
    "janky": "tỷ lệ khung hình trễ (jank, > 16,7 ms)",
    "p90": "thời gian xử lý khung hình (p90, CPU)",
    "heap": "Java heap (sau thao tác)",
    "rss": "mức dùng RAM (RSS anon, đỉnh)",
    "decoded": "dung lượng ảnh đã giải mã",
    "qmax": "truy vấn chậm nhất",
    "qsum": "tổng thời gian truy vấn",
    "apk": "dung lượng APK",
    "recomp": "thời gian recompose (luồng chính)",
}


def die(msg):
    print("LỖI: " + msg)
    sys.exit(1)


# ---------- SDK, tools, host ----------

def sdk_root():
    for k in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        v = os.environ.get(k)
        if v and Path(v).is_dir():
            return Path(v)
    lp = ANDROID / "local.properties"
    if lp.exists():
        m = re.search(r"^sdk\.dir=(.+)$", lp.read_text(encoding="utf-8", errors="replace"), re.M)
        if m:
            p = Path(m.group(1).strip().replace("\\:", ":").replace("\\\\", "\\"))
            if p.is_dir():
                return p
    cands = [Path.home() / "Android" / "Sdk", Path.home() / "Library" / "Android" / "sdk"]
    if os.environ.get("LOCALAPPDATA"):
        cands.insert(0, Path(os.environ["LOCALAPPDATA"]) / "Android" / "Sdk")
    return next((p for p in cands if p.is_dir()), None)


SDK = sdk_root()
EXE = ".exe" if WIN else ""


def tool(rel):
    return SDK / (rel + EXE) if SDK else None


ADB = tool("platform-tools/adb")
EMULATOR = tool("emulator/emulator")


def avd_home():
    if os.environ.get("ANDROID_AVD_HOME"):
        return Path(os.environ["ANDROID_AVD_HOME"])
    base = os.environ.get("ANDROID_USER_HOME") or os.environ.get("ANDROID_EMULATOR_HOME")
    return Path(base) / "avd" if base else Path.home() / ".android" / "avd"


def free_ram_mb():
    if sys.platform.startswith("linux"):
        m = re.search(r"MemAvailable:\s+(\d+)", Path("/proc/meminfo").read_text())
        return int(m.group(1)) // 1024 if m else None
    if WIN:
        import ctypes

        class MS(ctypes.Structure):
            _fields_ = [("len", ctypes.c_ulong), ("load", ctypes.c_ulong), ("total", ctypes.c_ulonglong), ("avail", ctypes.c_ulonglong),
                        ("tpage", ctypes.c_ulonglong), ("apage", ctypes.c_ulonglong), ("tvirt", ctypes.c_ulonglong),
                        ("avirt", ctypes.c_ulonglong), ("ext", ctypes.c_ulonglong)]
        ms = MS()
        ms.len = ctypes.sizeof(MS)
        ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(ms))
        return ms.avail // (1024 * 1024)
    return None


def java_major(java):
    try:
        out = subprocess.run([str(java), "-version"], capture_output=True, text=True, timeout=30).stderr
    except Exception:
        return None
    m = re.search(r'version "(\d+)', out)
    return int(m.group(1)) if m else None


def find_jdk21():
    """The build uses jvmToolchain(21) and no toolchain download, so Gradle needs a JDK 21. Android Studio's bundled JBR is one."""
    homes = []
    if os.environ.get("JAVA_HOME"):
        homes.append(Path(os.environ["JAVA_HOME"]))
    j = shutil.which("java")
    if j:
        homes.append(Path(j).resolve().parent.parent)
    pf = [Path(os.environ[k]) for k in ("ProgramFiles", "LOCALAPPDATA") if os.environ.get(k)]
    homes += [p / "Android" / "Android Studio" / "jbr" for p in pf] + [p / "Programs" / "Android Studio" / "jbr" for p in pf]
    homes += [Path.home() / "android-studio" / "jbr", Path("/opt/android-studio/jbr"), Path("/snap/android-studio/current/jbr"),
              Path("/Applications/Android Studio.app/Contents/jbr/Contents/Home")]
    homes += sorted(Path("/usr/lib/jvm").glob("*21*")) if Path("/usr/lib/jvm").is_dir() else []
    for h in homes:
        java = h / "bin" / ("java" + EXE)
        if java.exists() and java_major(java) == 21:
            return h
    return None


def run(cmd, timeout=600, check=False, **kw):
    r = subprocess.run([str(c) for c in cmd], capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout, **kw)
    if check and r.returncode != 0:
        die(f"{' '.join(map(str, cmd))} -> exit {r.returncode}\n{r.stdout[-2000:]}{r.stderr[-2000:]}")
    return r


# ---------- devices ----------

class Dev:
    def __init__(self, port):
        self.serial = f"emulator-{port}"

    def adb(self, *args, timeout=120, check=False):
        return run([ADB, "-s", self.serial, *args], timeout=timeout, check=check)

    def sh(self, cmd, timeout=120):
        return self.adb("shell", cmd, timeout=timeout).stdout

    def online(self):
        return self.adb("get-state", timeout=10).stdout.strip() == "device"

    def booted(self):
        return self.sh("getprop sys.boot_completed", timeout=10).strip() == "1"


def online_devices():
    """(gpu, [devices]) of the demo emulators that are up, or (None, [])."""
    if not ADB or not ADB.exists():
        return None, []
    listed = run([ADB, "devices"], timeout=30).stdout
    for gpu, ports in PORTS.items():
        devs = [Dev(p) for p in ports if re.search(rf"^emulator-{p}\s+device", listed, re.M)]
        if devs and devs[0].serial == f"emulator-{ports[0]}":
            return gpu, devs
    return None, []


def write_avd(name, slot):
    home = avd_home()
    d = home / f"{name}.avd"
    d.mkdir(parents=True, exist_ok=True)
    sysdir = os.sep.join(["system-images", "android-34", "google_apis", "x86_64", ""])
    cfg = {  # the study's lowend34 profile: API 34, 2 cores, 2 GB, 720x1280 at 280 dpi
        "AvdId": name, "avd.ini.displayname": name, "abi.type": "x86_64", "hw.cpu.arch": "x86_64", "hw.cpu.ncore": "2",
        "hw.ramSize": "2048", "vm.heapSize": "256", "hw.lcd.width": "720", "hw.lcd.height": "1280", "hw.lcd.density": "280",
        "hw.keyboard": "yes", "hw.gpu.enabled": "yes", "hw.gpu.mode": "swiftshader_indirect", "hw.audioInput": "no",
        "hw.initialOrientation": "portrait", "disk.dataPartition.size": "4G", "image.sysdir.1": sysdir, "tag.id": "google_apis",
        "tag.display": "Google APIs", "target": "android-34", "PlayStore.enabled": "false", "showDeviceFrame": "no",
    }
    (d / "config.ini").write_text("".join(f"{k}={v}\n" for k, v in cfg.items()), encoding="utf-8")
    user = d / "emulator-user.ini"
    if not user.exists():  # first boot only: A on the left, B on the right; the emulator remembers where you drag them
        user.write_text(f"window.x = {20 + slot * 640}\nwindow.y = 20\n", encoding="utf-8")
    (home / f"{name}.ini").write_text(f"avd.ini.encoding=UTF-8\npath={d}\npath.rel=avd/{name}.avd\ntarget=android-34\n", encoding="utf-8")


def headless_default():
    return sys.platform.startswith("linux") and not (os.environ.get("DISPLAY") or os.environ.get("WAYLAND_DISPLAY"))


def boot(count, gpu, headless):
    if gpu not in PORTS:
        die(f"--gpu phải là một trong {', '.join(PORTS)}")
    for a in AVDS[:count]:
        if not (avd_home() / f"{a}.ini").exists():
            die(f"chưa có AVD {a}: chạy 'python demo.py setup' trước")
    (OUT / "logs").mkdir(parents=True, exist_ok=True)
    devs = []
    for i, (avd, port) in enumerate(zip(AVDS[:count], PORTS[gpu])):
        cmd = [EMULATOR, "-avd", avd, "-port", str(port), "-gpu", gpu, "-no-snapshot", "-no-boot-anim", "-no-audio",
               "-memory", "2048", "-cores", "2", "-no-metrics"] + (["-no-window"] if headless else [])
        log = open(OUT / "logs" / f"emulator-{avd}.log", "w")
        flags = dict(creationflags=subprocess.CREATE_NEW_PROCESS_GROUP | 0x00000008) if WIN else dict(start_new_session=True)  # DETACHED_PROCESS
        subprocess.Popen([str(c) for c in cmd], stdout=log, stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL, **flags)
        devs.append(Dev(port))
        print(f"  khởi động {avd} ({'A' if i == 0 else 'B'}) cổng {port}, GPU {gpu}{', không cửa sổ' if headless else ''}")
    t0 = time.time()
    for d in devs:
        while not (d.online() and d.booted()):
            if time.time() - t0 > 420:
                die(f"{d.serial} không khởi động xong sau 7 phút; xem {OUT / 'logs'}")
            time.sleep(3)
    for d in devs:  # keep the screen on and unlocked for the whole talk
        d.sh("svc power stayon true; settings put system screen_off_timeout 2147483647; input keyevent 82; wm dismiss-keyguard")
    print(f"  đã sẵn sàng sau {time.time() - t0:.0f} s")
    return devs


def stop_all():
    gpu, devs = online_devices()
    while devs:
        for d in devs:
            d.adb("emu", "kill", timeout=30)
        t0 = time.time()
        while any(d.online() for d in devs) and time.time() - t0 < 60:
            time.sleep(2)
        gpu, devs = online_devices()


def provision(d, force=False):
    """Installs both APKs, compiles like the study (speed for the corpus screens), seeds covers + the 100k-book DB, warms up P13 A."""
    have = d.sh("pm list packages shelftap.optlab")
    if force or f"package:{PKG}\n" not in have + "\n" or f"package:{PKG_NOR8}" not in have:
        for key in ("release", "noR8"):
            apk = OUT / "apk" / Path(APKS[key]).name
            if not apk.exists():
                die(f"thiếu {apk}: chạy 'python demo.py setup'")
            r = d.adb("install", "-r", "-t", str(apk), timeout=300)
            if "Success" not in r.stdout:
                die(f"cài {apk.name} lên {d.serial} lỗi: {r.stdout}{r.stderr}")
        d.sh(f"cmd package compile -m speed -f {PKG}", timeout=600)
    d.adb("logcat", "-c")
    d.sh(f"am force-stop {PKG}; am start -W -n {PKG}/{ACT} --es factor seed")
    t0 = time.time()
    while "seeded" not in d.adb("logcat", "-d", "-s", "STLAB:I").stdout:
        if "seed failed" in d.adb("logcat", "-d").stdout or time.time() - t0 > 900:
            die(f"tạo dữ liệu trên {d.serial} lỗi; xem 'adb -s {d.serial} logcat -s STLAB'")
        time.sleep(2)
    for v in "AB":  # warm-up after every boot: A builds its titles table once, and both runs pull the search data into the page cache
        d.sh(f"am force-stop {PKG}; am start -W -n {PKG}/{ACT} --es factor P13c --es variant {v}; sleep 4; "
             f"input tap 360 190; input text silent%sriver; sleep 4; am force-stop {PKG}", timeout=180)


def ensure(scn_gpu, want=None, headless=None):
    """Devices on the scenario's GPU, booting or rebooting the pair/single set as needed. want: 1, 2 or None (keep / auto)."""
    gpu, devs = online_devices()
    headless = headless_default() if headless is None else headless
    if devs and gpu == scn_gpu and (want is None or want == len(devs)):
        return devs
    count = want or (len(devs) if devs else (2 if (free_ram_mb() or 0) >= PAIR_MIN_MB else 1))
    if devs:
        why = f"kịch bản cần GPU {scn_gpu}" if gpu != scn_gpu else f"chuyển sang {count} máy ảo"
        print(f"{why[0].upper() + why[1:]}: tắt máy ảo đang chạy và khởi động lại (khoảng 1 phút)...")
        stop_all()
    devs = boot(count, scn_gpu, headless)
    for d in devs:
        provision(d)
    return devs


# ---------- measuring ----------

def pid_of(d, pkg):
    return d.sh(f"pidof {pkg}").strip().split(" ")[0]


def parse_heap_kb(txt):
    for line in txt.splitlines():
        if line.strip().startswith("Dalvik Heap"):
            nums = [int(x) for x in re.findall(r"\d+", line)]
            if len(nums) >= 3:
                return nums[-3]  # Heap Size column
    return None


TRACE_LINE = re.compile(r"-(\d+)\s+\(\s*[\d-]+\)\s+\[\d+\]\s+(?:\S+\s+)?([\d.]+): tracing_mark_write: (.*)$")


def pct(vals, q):
    """Linear-interpolated percentile, as run_bench.py computes it."""
    s = sorted(vals)
    k = (len(s) - 1) * q
    lo = int(k)
    hi = min(lo + 1, len(s) - 1)
    return s[lo] + (s[hi] - s[lo]) * (k - lo)


def parse_trace(txt, pid, since):
    """From atrace text, the app's (tgid = pid) probe counters (max), st.search.query durations (ms), and per-frame CPU durations (ms)
    as Macrobenchmark's FrameTimingMetric defines them: Choreographer#doFrame <id> start to the end of RenderThread DrawFrames <id>,
    for frames that start after since (device boot clock, seconds), minus the time RenderThread sat in eglSwapBuffers: on the emulator
    that wait is display back-pressure (it flips between ~0 and one vsync from run to run), not the app's work."""
    counters, queries, stacks, starts, ends, swap = {}, [], {}, {}, {}, {}
    recomp = 0.0
    for line in txt.splitlines():
        m = TRACE_LINE.search(line)
        if not m:
            continue
        tid, ts, parts = m.group(1), float(m.group(2)), m.group(3).strip().split("|")
        if len(parts) < 2 or parts[1] != pid:
            continue
        if parts[0] == "B" and len(parts) >= 3:
            stacks.setdefault(tid, []).append((parts[2], ts))
            if parts[2].startswith("Choreographer#doFrame "):
                starts.setdefault(parts[2].split()[-1], ts)
        elif parts[0] == "E" and stacks.get(tid):
            name, t0 = stacks[tid].pop()
            if name == "st.search.query":
                queries.append((ts - t0) * 1000)
            elif name == "Compose:recompose" and tid == pid:  # whole run from launch, like the study's Compose_recompose.mainMs
                recomp += (ts - t0) * 1000
            elif name.startswith("DrawFrames "):
                ends[name.split()[-1]] = ts
            elif name.startswith("eglSwapBuffers"):  # time blocked handing the buffer to the emulator's renderer
                frame = next((n for n, _ in reversed(stacks[tid]) if n.startswith("DrawFrames ")), None)
                if frame:
                    v = frame.split()[-1]
                    swap[v] = swap.get(v, 0) + ts - t0
        elif parts[0] == "C" and len(parts) >= 4 and parts[2].startswith("st."):
            try:
                counters[parts[2]] = max(counters.get(parts[2], 0), float(parts[3]))
            except ValueError:
                pass
    frames = [(ends[v] - t - swap.get(v, 0)) * 1000 for v, t in starts.items() if v in ends and t >= since]
    raw = [(ends[v] - t) * 1000 for v, t in starts.items() if v in ends and t >= since]
    return counters, queries, frames, raw, recomp


class Sampler(threading.Thread):
    """Peak RssAnon of the app while the gesture runs (the study reports the maximum too)."""

    def __init__(self, d, pkg):
        super().__init__(daemon=True)
        self.d, self.pkg, self.stop, self.peak = d, pkg, threading.Event(), 0

    def run(self):
        pid = pid_of(self.d, self.pkg)
        while pid and not self.stop.is_set():
            m = re.search(r"RssAnon:\s+(\d+)", self.d.sh(f"cat /proc/{pid}/status", timeout=10))
            if m:
                self.peak = max(self.peak, int(m.group(1)))
            self.stop.wait(0.5)


def launch(d, pkg, factor, variant):
    d.sh(f"am force-stop {PKG}; am force-stop {PKG_NOR8}")
    out = d.sh(f"am start -W -n {pkg}/{ACT} --es factor {factor} --es variant {variant}", timeout=180)
    m = re.search(r"TotalTime: (\d+)", out)
    return int(m.group(1)) if m else None


def measure(d, scn, variant, barrier=None, record=None):
    """One side of a scenario on one device. Returns the live metrics dict."""
    s = SCENARIOS[scn]
    res = {}
    if scn == "p10":
        pkg = PKG_NOR8 if variant == "A" else PKG
        d.sh(f"cmd package compile -m verify -f {pkg}", timeout=600)  # the study's CompilationMode.None for both builds
        if barrier:
            barrier.wait()
        times = [launch(d, pkg, "startup", "B") for _ in range(5)]
        times = [t for t in times if t]
        res["ttid"] = statistics.median(times) if times else None
        apk = OUT / "apk" / Path(APKS["noR8" if variant == "A" else "release"]).name
        res["apk"] = apk.stat().st_size if apk.exists() else None
        if variant == "B":
            d.sh(f"cmd package compile -m speed -f {PKG}", timeout=600)  # back to the state the other scenarios expect
        d.sh(f"am force-stop {pkg}")
        return res
    d.sh("atrace --async_stop > /dev/null 2>&1")
    d.sh(f"atrace --async_start -b 32768 -a {PKG} view gfx")  # app probes (st.*) and the frame slices, as the study's Perfetto trace
    if barrier:
        barrier.wait()
    res["ttid"] = launch(d, PKG, s["factor"], variant)
    time.sleep(4)  # let the screen settle (P13 opens its database)
    pid = pid_of(d, PKG)
    sampler = Sampler(d, PKG)
    sampler.start()
    rec = None
    if record:
        rec = subprocess.Popen([str(ADB), "-s", d.serial, "shell", "screenrecord", "--size", "720x1280", "--bit-rate", "3000000",
                                "--time-limit", "20", "/sdcard/demo.mp4"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        time.sleep(1)
    since = float(d.sh("cat /proc/uptime").split()[0])  # same clock as the trace (boot)
    d.sh(s["gesture"], timeout=180)
    sampler.stop.set()
    sampler.join(15)
    if rec:
        d.sh("pkill -INT screenrecord")
        rec.wait(30)
        time.sleep(1)
        d.adb("pull", "/sdcard/demo.mp4", str(record), timeout=120)
        d.sh("rm -f /sdcard/demo.mp4")
    res["heap"] = parse_heap_kb(d.sh(f"dumpsys meminfo {PKG}"))
    res["rss"] = sampler.peak or None
    counters, queries, frames, raw, res["recomp"] = parse_trace(d.sh("atrace --async_stop", timeout=180), pid, since)
    res["rows"] = counters.get("st.rc.row")
    res["decoded"] = counters.get("st.decodedBytes")
    if queries:
        res["qmax"], res["qsum"], res["qn"] = max(queries), sum(queries), len(queries)
    res["frames"] = len(frames)
    if frames:
        res["p90"] = pct(frames, 0.9)
        res["janky"] = 100.0 * sum(1 for f in frames if f > 16.67) / len(frames)
        res["p90_raw"] = pct(raw, 0.9)
        res["janky_raw"] = 100.0 * sum(1 for f in raw if f > 16.67) / len(raw)
    d.sh(f"am force-stop {PKG}")
    return res


def vn(x, digits=1):
    """Vietnamese number format: decimal comma, dot for thousands."""
    s = f"{x:,.{digits}f}" if digits else f"{round(x):,}"
    return s.replace(",", "_").replace(".", ",").replace("_", ".")


def show(key, v):
    if v is None:
        return "—"
    if key in ("heap", "rss"):
        mb = v / 1024
        return f"{vn(mb, 1 if mb < 10 else 0)} MB"
    if key == "decoded":
        mb = v / 1e6
        return f"{vn(mb, 1 if mb < 10 else 0)} MB"
    if key == "apk":
        return f"{vn(v / 1e6, 1)} MB"
    if key == "janky":
        return f"{vn(v, 1)}%"
    if key == "rows":
        return vn(v, 0)
    if key == "qsum":
        return f"{vn(v, 0)} ms"
    return f"{vn(v, 0)} ms"


def table(scn, a, b, mode):
    s = SCENARIOS[scn]
    rows = [(LABELS[k], show(k, a.get(k)), show(k, b.get(k)), s["study"].get(k, "—")) for k in s["live"]]
    w = [max(len(r[i]) for r in rows + [("Chỉ số", "A chưa tối ưu", "B đã tối ưu", "Nghiên cứu A → B")]) for i in range(4)]
    line = lambda r: "  ".join(c.ljust(w[i]) for i, c in enumerate(r))
    print()
    print(line(("Chỉ số", "A chưa tối ưu", "B đã tối ưu", "Nghiên cứu A → B")))
    print(line(tuple("-" * x for x in w)))
    for r in rows:
        print(line(r))
    if a.get("qn") or b.get("qn"):
        print(f"(số truy vấn: A {a.get('qn', 0)}, B {b.get('qn', 0)})")
    if "janky" in s["live"]:
        print(f"(số khung hình: A {a.get('frames', 0)}, B {b.get('frames', 0)}; dưới 20 khung hình thì tỷ lệ jank không có ý nghĩa)")
    print(f"\nChế độ {mode}. Số liệu trực tiếp chỉ để minh họa: chỉ so A với B trên cùng máy ảo. Số liệu tham chiếu là của nghiên cứu.")
    if mode.startswith("hai"):
        print("Hai máy ảo chạy cùng lúc nên tranh CPU của máy tính: số đếm (mục, MB) tin được, số thời gian (ms, %) có thể lệch. "
              "Cần đọc số thời gian thì chạy lại với --single.")


def run_scenario(scn, record=False, want=None, headless=None):
    s = SCENARIOS[scn]
    devs = ensure(s.get("gpu", "swiftshader_indirect"), want, headless)
    mode = "hai máy ảo (trái A, phải B)" if len(devs) == 2 else "một máy ảo (A rồi B)"
    print(f"\n== {scn}: {s['title']}\n   Slide {s['slides']}. {s['say']}\n   {mode}")
    rec = {}
    if record:
        (OUT / "rec").mkdir(parents=True, exist_ok=True)
        rec = {v: OUT / "rec" / f"{scn}-{v}.mp4" for v in "AB"}
    if len(devs) == 2:
        results, barrier = {}, threading.Barrier(2)

        def side(d, v):
            results[v] = measure(d, scn, v, barrier, rec.get(v))
        ts = [threading.Thread(target=side, args=(d, v)) for d, v in zip(devs, "AB")]
        for t in ts:
            t.start()
        for t in ts:
            t.join()
        a, b = results.get("A", {}), results.get("B", {})
    else:
        print("   A đang chạy...")
        a = measure(devs[0], scn, "A", record=rec.get("A"))
        print("   B đang chạy...")
        b = measure(devs[0], scn, "B", record=rec.get("B"))
    table(scn, a, b, mode)
    if record:
        files = [str(p) for p in rec.values() if p.exists()]
        print("\nĐã ghi: " + ", ".join(files))
        ff = shutil.which("ffmpeg")
        if ff and len(files) == 2:
            dst = OUT / "rec" / f"{scn}-AB.mp4"
            r = run([ff, "-y", "-loglevel", "error", "-i", rec["A"], "-i", rec["B"], "-filter_complex", "[0:v][1:v]hstack=inputs=2:shortest=1",
                     "-c:v", "libx264", "-pix_fmt", "yuv420p", dst], timeout=600)
            print(f"Video ghép A|B: {dst}" if r.returncode == 0 else f"ffmpeg lỗi: {r.stderr[-500:]}")
        elif not ff:
            print("Không có ffmpeg: hai tệp A và B riêng (mở cạnh nhau, hoặc cài ffmpeg rồi ghi lại).")
    json_out = OUT / "last-run.json"
    json_out.parent.mkdir(parents=True, exist_ok=True)
    json_out.write_text(json.dumps({"scenario": scn, "mode": mode, "A": a, "B": b, "at": time.strftime("%Y-%m-%d %H:%M:%S")}, indent=1))


# ---------- commands ----------

def gradle_build():
    jdk = find_jdk21()
    if not jdk:
        die("không tìm thấy JDK 21 (cần cho Gradle). Cài JDK 21 hoặc trỏ JAVA_HOME tới thư mục jbr của Android Studio.")
    env = dict(os.environ, ANDROID_HOME=str(SDK), JAVA_HOME=str(jdk))
    cmd = ["cmd", "/c", str(ANDROID / "gradlew.bat")] if WIN else [str(ANDROID / "gradlew")]
    print(f"Build optlab (release + noR8) bằng JDK {jdk} ... lần đầu mất vài phút")
    # no daemons left behind (they hold about 2 GB the emulators need); configure-on-demand keeps :app's configuration out of it
    r = subprocess.run(cmd + [":optlab:assembleRelease", ":optlab:assembleNoR8", "--console=plain", "--no-daemon", "--configure-on-demand",
                              "-Pkotlin.compiler.execution.strategy=in-process"], cwd=ANDROID, env=env)
    if r.returncode != 0:
        die("Gradle build lỗi (xem log phía trên)")
    (OUT / "apk").mkdir(parents=True, exist_ok=True)
    for rel in APKS.values():
        shutil.copy2(ANDROID / rel, OUT / "apk" / Path(rel).name)
        print(f"  {Path(rel).name}: {vn((ANDROID / rel).stat().st_size / 1e6, 1)} MB")


def cmd_doctor(_):
    ok = True

    def check(name, good, detail, required=True):
        nonlocal ok
        mark = "OK " if good else ("LỖI" if required else "--")
        ok = ok and (good or not required)
        print(f"[{mark}] {name}: {detail}")

    check("Python", sys.version_info >= (3, 8), sys.version.split()[0])
    check("Android SDK", bool(SDK), str(SDK) if SDK else "không thấy; đặt ANDROID_HOME (Android Studio > Settings > Android SDK)")
    if not SDK:
        return sys.exit(1)
    check("adb", ADB.exists(), str(ADB) if ADB.exists() else "cài 'Android SDK Platform-Tools' trong SDK Manager")
    check("emulator", EMULATOR.exists(), str(EMULATOR) if EMULATOR.exists() else "cài 'Android Emulator' trong SDK Manager")
    img = SDK / "system-images" / "android-34" / "google_apis" / "x86_64"
    check("system image", (img / "system.img").exists(), str(img) if img.exists() else f"cài bằng: sdkmanager \"{IMAGE}\" (setup tự thử)")
    if EMULATOR.exists():
        r = run([EMULATOR, "-accel-check"], timeout=60)
        txt = [x for x in (r.stdout + r.stderr).splitlines() if x.strip()]
        msg = next((x for x in txt if re.search(r"usable|installed|KVM|WHPX|AEHD|HAXM|HVF", x)), txt[-1] if txt else "")
        check("tăng tốc phần cứng", r.returncode == 0, msg.strip() or f"exit {r.returncode}")
    jdk = find_jdk21()
    check("JDK 21", bool(jdk), str(jdk) if jdk else "cần JDK 21: cài Temurin 21 hoặc trỏ JAVA_HOME tới <Android Studio>/jbr")
    mb = free_ram_mb()
    cores = os.cpu_count() or 0
    pair = (mb or 0) >= PAIR_MIN_MB
    gpu, devs = online_devices()
    if devs:
        check("RAM trống", True, f"{mb} MB khi {len(devs)} máy ảo demo đang chạy (đo lại sau 'stop' để biết chế độ mặc định)")
    else:
        check("RAM trống", (mb or 0) >= 3500, f"{mb} MB, {cores} nhân CPU -> mặc định {'hai máy ảo (pair)' if pair else 'một máy ảo (single)'}"
              + ("" if pair else f"; cần >= {PAIR_MIN_MB} MB trống để chạy hai máy ảo"))
    free_gb = shutil.disk_usage(avd_home() if avd_home().exists() else Path.home()).free / 1e9
    check("ổ đĩa cho AVD", free_gb >= 10, f"{free_gb:.0f} GB trống tại {avd_home()} (cần khoảng 10 GB cho hai AVD)")
    for a in AVDS:
        check(f"AVD {a}", (avd_home() / f"{a}.ini").exists(), "có" if (avd_home() / f"{a}.ini").exists() else "chưa có, setup sẽ tạo", required=False)
    for rel in APKS.values():
        p = OUT / "apk" / Path(rel).name
        check(f"APK {p.name}", p.exists(), f"{vn(p.stat().st_size / 1e6, 1)} MB" if p.exists() else "chưa build, setup sẽ build", required=False)
    check("ffmpeg (tùy chọn, ghép video)", bool(shutil.which("ffmpeg")), shutil.which("ffmpeg") or "không có; record vẫn ghi hai tệp A, B riêng", required=False)
    check("máy ảo đang chạy", bool(devs), f"{len(devs)} máy, GPU {gpu}: {', '.join(d.serial for d in devs)}" if devs else "chưa chạy", required=False)
    print("\nKết quả: " + ("ĐẠT" if ok else "CHƯA ĐẠT, sửa các dòng LỖI rồi chạy lại doctor"))
    sys.exit(0 if ok else 1)


def cmd_setup(a):
    if not SDK or not ADB.exists() or not EMULATOR.exists():
        die("thiếu Android SDK, adb hoặc emulator: chạy 'python demo.py doctor'")
    img = SDK / "system-images" / "android-34" / "google_apis" / "x86_64" / "system.img"
    if not img.exists():
        sm = SDK / "cmdline-tools" / "latest" / "bin" / ("sdkmanager.bat" if WIN else "sdkmanager")
        if not sm.exists():
            die(f"thiếu system image {IMAGE}; cài qua Android Studio > SDK Manager > SDK Platforms (Show Package Details)")
        print(f"Tải {IMAGE} (khoảng 1,5 GB)...")
        subprocess.run((["cmd", "/c"] if WIN else []) + [str(sm), IMAGE])
    if not a.skip_build:
        gradle_build()
    for i, name in enumerate(AVDS):
        write_avd(name, i)
    print(f"AVD: {', '.join(AVDS)} trong {avd_home()}")
    stop_all()
    count = 1 if a.single else 2 if a.pair else None
    count = count or (2 if (free_ram_mb() or 0) >= PAIR_MIN_MB else 1)
    devs = boot(count, "swiftshader_indirect", a.headless or headless_default())
    for d in devs:
        print(f"  cài APK, biên dịch, tạo dữ liệu trên {d.serial} (lần đầu vài phút)...")
        provision(d, force=True)
    print(f"\nXong. Thử: {'py' if WIN else 'python3'} demo.py run p09")


def cmd_start(a):
    want = 1 if a.single else 2 if a.pair else None
    devs = ensure(a.gpu, want, a.headless or None)
    print(f"Đang chạy: {', '.join(d.serial for d in devs)} (GPU {a.gpu})")


def cmd_list(_):
    for k, s in SCENARIOS.items():
        clip = f"evidence/video/{s['clip']}" if s["clip"] else "không có video; dùng bảng số của nghiên cứu"
        print(f"{k:6} slide {s['slides']:7} {s['title']}\n       dự phòng: {clip}")


def cmd_run(a, record=False):
    if a.scenario not in SCENARIOS:
        die(f"không có kịch bản '{a.scenario}'; xem 'python demo.py list'")
    run_scenario(a.scenario, record, 1 if a.single else 2 if a.pair else None, a.headless or None)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)

    def modes(p):
        g = p.add_mutually_exclusive_group()
        g.add_argument("--single", action="store_true", help="one emulator, A then B")
        g.add_argument("--pair", action="store_true", help="two emulators, A left and B right, at the same time")
        p.add_argument("--headless", action="store_true", help="no emulator window (default on Linux without a display)")
    sub.add_parser("doctor")
    p = sub.add_parser("setup")
    modes(p)
    p.add_argument("--skip-build", action="store_true", help="reuse the APKs in out/apk")
    p = sub.add_parser("start")
    modes(p)
    p.add_argument("--gpu", default="swiftshader_indirect", choices=list(PORTS))
    sub.add_parser("list")
    for name in ("run", "record"):
        p = sub.add_parser(name)
        p.add_argument("scenario", help=", ".join(SCENARIOS))
        modes(p)
    sub.add_parser("stop")
    a = ap.parse_args()
    if a.cmd not in ("doctor", "list") and not SDK:
        die("không thấy Android SDK: chạy 'python demo.py doctor'")
    {"doctor": cmd_doctor, "setup": cmd_setup, "start": cmd_start, "list": cmd_list, "run": cmd_run,
     "record": lambda x: cmd_run(x, record=True), "stop": lambda _: (stop_all(), print("Đã tắt máy ảo demo."))}[a.cmd](a)


if __name__ == "__main__":
    main()
