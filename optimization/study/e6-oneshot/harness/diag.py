#!/usr/bin/env python3
"""E6 diagnostics (NOT part of runs.csv): why did a run crash at launch / show an empty list. Usage: diag.py <run_id> [wait_s]
Installs the run's APK, launches it, saves the crash-buffer logcat and a screenshot after wait_s (default 25) to results/diag/<run_id>.{txt,jpg}."""
import sys, time, pathlib, re
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import e6
from PIL import Image

def main():
    runs = sys.argv[1].split(","); wait = int(sys.argv[2]) if len(sys.argv) > 2 else 25
    out = e6.E6 / "results" / "diag"; out.mkdir(exist_ok=True)
    with e6.exclusive_bench():
        p = e6.emu_up()
        try:
            for run in runs:
                apk = e6.WORK / "apk-corpus" / f"{run}.apk"
                pkg = e6.subprocess.run([e6.APKANALYZER, "manifest", "application-id", str(apk)], capture_output=True, text=True).stdout.strip()
                e6.adb("uninstall", pkg); e6.adb("install", "-r", "-t", str(apk), timeout=180)
                comp = e6.adb("shell", "cmd", "package", "resolve-activity", "--brief", "-c", "android.intent.category.LAUNCHER", pkg).strip().splitlines()[-1].strip()
                e6.adb("shell", "settings", "put", "system", "font_scale", "1.0"); e6.adb("logcat", "-c"); e6.adb("shell", "am", "force-stop", pkg)
                e6.adb("shell", "am", "start", "-W", "-n", comp); time.sleep(wait)
                log = e6.adb("logcat", "-d", "-b", "crash") + "\n--- main (AndroidRuntime/ANR/pkg errors) ---\n" + "\n".join(l for l in e6.adb("logcat", "-d", "-b", "main", "*:W").splitlines() if pkg in l or "AndroidRuntime" in l or "ANR" in l)[-4000:]
                (out / f"{run}.txt").write_text(f"pkg {pkg}\nalive_after_{wait}s {bool(e6.adb('shell', 'pidof', pkg).strip())}\n" + log[-6000:])
                png = out / f"{run}.png"; e6.shot(png)
                im = Image.open(png).convert("RGB"); im = im.resize((360, int(im.height * 360 / im.width))); im.save(out / f"{run}.jpg", quality=75); png.unlink()
                e6.adb("uninstall", pkg); print(run, "done", flush=True)
        finally:
            e6.emu_down(p)

if __name__ == "__main__":
    main()
