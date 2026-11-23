#!/usr/bin/env python3
"""Demo clips for the class video: adb screenrecord of the A (slop) and B (tuned) screens, same finger input for both.

Needs the emulator up and nothing else measuring (recording perturbs timing, so never run it during run_bench.py).
Factors are the corpus ids (A = corpus code, B = tuned). Output: <out>/<factor>-<variant>-<action>.mp4, 720x1280, ~14 s each. Usage: python3 record_demo.py [--out DIR] [--factors P08,P13]
"""
import argparse, os, subprocess, time
from pathlib import Path

ADB = os.path.expanduser("~/Android/Sdk/platform-tools/adb")
SERIAL = os.environ.get("ANDROID_SERIAL", "emulator-5570")
PKG = "shelftap.optlab"
SWIPES = "; ".join(["input swipe 360 1000 360 350 250; sleep 0.8"] * 8)
# one on-device shell line per action, so adb round-trips do not add jitter between keystrokes
ACTIONS = {
    "P08c": ("scroll", SWIPES),  # record on the swangle_indirect emulator: the corpus A crashes swiftshader_indirect (A14)
    "P06c": ("scroll", SWIPES),
    "P09c": ("scroll", SWIPES),
    "P01c": ("scroll", SWIPES),
    "P03P04c": ("scroll", "; ".join(["input swipe 360 1000 360 350 250; sleep 0.8"] * 2) + "; sleep 8"),
    "P13c": ("search", "input tap 360 190; sleep 1; for c in s i l e n t %s r i v e r; do input text $c; sleep 0.15; done; sleep 3"),
}


def adb(*args, **kw):
    return subprocess.run([ADB, "-s", SERIAL, *args], capture_output=True, text=True, **kw)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="/mnt/01DC198BE9192500/github/Schooldev/3S-study-work/video")
    ap.add_argument("--factors", default=",".join(ACTIONS))
    args = ap.parse_args()
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    for factor in args.factors.split(","):
        action, script = ACTIONS[factor]
        for variant in ("A", "B"):
            adb("shell", f"am force-stop {PKG}")
            adb("shell", f"am start -W -n {PKG}/shelftap.optlab.MainActivity --es factor {factor} --es variant {variant}")
            time.sleep(4)  # let the screen settle (P13 loads its data)
            rec = subprocess.Popen([ADB, "-s", SERIAL, "shell", "screenrecord", "--size", "720x1280", "--bit-rate", "3000000",
                                    "--time-limit", "14", "/sdcard/demo.mp4"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            time.sleep(1)
            adb("shell", script, timeout=60)
            rec.wait(timeout=60)
            time.sleep(1)
            dst = out / f"{factor}-{variant}-{action}.mp4"
            adb("pull", "/sdcard/demo.mp4", str(dst))
            adb("shell", "rm -f /sdcard/demo.mp4")
            print(dst, dst.stat().st_size)


if __name__ == "__main__":
    main()
