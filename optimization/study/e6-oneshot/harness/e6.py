#!/usr/bin/env python3
"""E6 harness: one-shot hype prompt x 4 bundles x 3 arms x 3 reps, generate -> build -> emulator -> detector -> csv.

  e6.py canary                     isolation canary for every bundle (results/canary.json)
  e6.py plan                       shuffled plan (seed 20261127, tier 1 = arm O, tier 2 = R and S) -> results/plan.json
  e6.py gen     [--tier N] [--workers 2]
  e6.py build   [--tier N] [--follow]
  e6.py measure [--tier N]         emulator pass (exclusive bench.lock); boots and stops the emulator itself
  e6.py scan    [--tier N]
  e6.py collect                    results/runs.csv, summary.csv, findings.csv, repairs.csv, img/*.jpg, grids

Isolation: generation only through e4-pressure/harness/gen_launch.py (stub HOME), imported read-only together with the E1 helpers
(generate.py, canary.py, build_run.py, scan_corpus.py). Nothing under e1-corpus/ or e4-pressure/ is edited; this file patches module-level
paths inside its OWN process (build_run.E1, canary.WORKDIR). Heavy state (projects, transcripts, APKs, raw screenshots) lives in WORK/e6.
"""
import sys
sys.dont_write_bytecode = True          # never leave __pycache__ inside the E1/E4 trees we import read-only
import argparse, concurrent.futures as cf, csv, fcntl, gzip, json, os, pathlib, random, re, shutil, statistics, subprocess, sys, time

HERE = pathlib.Path(__file__).resolve().parent
E6 = HERE.parent
STUDY = E6.parent
WORK = pathlib.Path("/mnt/01DC198BE9192500/github/Schooldev/3S-study-work")
H = WORK / "e6"                     # heavy state
RUNS = H / "runs"
RES = E6 / "results"
IMG = E6 / "img"
for p in (E6 / "harness", STUDY / "e1-corpus" / "harness", STUDY / "e4-pressure" / "harness", STUDY / "e1-corpus" / "slop-scan"):
    sys.path.insert(0, str(p))
import gen_launch, generate, canary, build_run, scan_corpus  # noqa: E402  (read-only reuse of E1/E4 code)

# --- E6-local launcher patch (module-level, this process only; e4-pressure/e1-corpus files untouched) ----------------------------------
# agy 1.5.18 keeps its login in ~/.gemini/antigravity-cli/antigravity-oauth-token (1.5.13 used ~/.gemini/oauth_creds.json); the launcher copies only the old file.
# Known, NOT fixable from inside the pool: /etc/claude-code/managed-settings.json force-loads three factory mods (compact-pilot, model-gate, usage-meter) into
# every claude run whatever HOME is (E1 canary 2026-11-21 shows the same list; CLAUDE_CODE_MANAGED_SETTINGS_PATH is not honoured for it: debug log still
# reads /etc/claude-code). Their hooks inject no model-visible text for these runs (see AMENDMENT addendum); the canary records them as a limitation.
FACTORY_MODS = ("compact-pilot", "model-gate", "usage-meter", "seat-bg-gate")
gen_launch.AUTH[".gemini/antigravity-cli/antigravity-oauth-token"] = lambda: gen_launch.REAL_HOME / ".gemini" / "antigravity-cli" / "antigravity-oauth-token"

scan_corpus.E1 = E6                     # depcheck refreshes <E1>/results/maven-snapshot in place: point it at E6's own copy (seeded from E1's committed snapshot) so E1's files stay untouched

SEED = 20261127
BUNDLES = {"claude-haiku": ("claude", "haiku"), "codex-luna": ("codex", "gpt-5.6-luna"),
           "agy-g38flash": ("agy", "gemini-3.8-flash-medium"), "claude-sonnet": ("claude", "claude-sonnet-5-5")}
ARMS = {"O": ("O.txt", False), "R": ("O.txt", True), "S": ("S.txt", False)}   # arm -> (prompt file, guardrail delivered as context file)
REPS = 3
TIMEOUT_S = generate.TIMEOUT_S

ADB = str(pathlib.Path.home() / "Android/Sdk/platform-tools/adb")
EMU = str(pathlib.Path.home() / "Android/Sdk/emulator/emulator")
APKANALYZER = str(pathlib.Path.home() / "Android/Sdk/cmdline-tools/latest/bin/apkanalyzer")
SERIAL, PORT = "emulator-5570", "5570"
AVD_HOME = WORK / "avd"            # lowend34: the E2 low-end rig (API 34 google_apis x86_64, 720x1280@280); test34 (1080x2280) saturated gfxinfo jank at ~100% in a 6-run smoke
EMU_FLAGS = ["-avd", "lowend34", "-no-snapshot", "-no-boot-anim", "-no-audio", "-no-window", "-memory", "2048", "-cores", "2", "-gpu", "swangle_indirect", "-port", PORT]

def rid(b, a, r): return f"{b}-{a}-r{r}"

def plan():
    rng = random.Random(SEED)
    t1 = [(b, "O", r) for b in BUNDLES for r in range(1, REPS + 1)]
    t2 = [(b, a, r) for b in BUNDLES for a in ("R", "S") for r in range(1, REPS + 1)]
    rng.shuffle(t1); rng.shuffle(t2)
    cells = [{"order": i + 1, "tier": 1 if c[1] == "O" else 2, "bundle": c[0], "arm": c[1], "rep": c[2], "run_id": rid(*c)} for i, c in enumerate(t1 + t2)]
    return cells

def load_plan():
    f = RES / "plan.json"
    if not f.exists(): save_plan()
    return json.loads(f.read_text())["cells"]

def save_plan():
    RES.mkdir(parents=True, exist_ok=True)
    (RES / "plan.json").write_text(json.dumps({"seed": SEED, "bundles": BUNDLES, "arms": ARMS, "reps": REPS, "cells": plan()}, indent=0))

def tier_cells(tier):
    return [c for c in load_plan() if not tier or c["tier"] == tier]

# ---------------------------------------------------------------- canary
def cmd_canary(a):
    canary.WORKDIR = H / "canary"
    jobs = []
    for name, (tool, model) in [("claude-haiku", BUNDLES["claude-haiku"]), ("claude-sonnet", BUNDLES["claude-sonnet"]), ("codex-luna", BUNDLES["codex-luna"]), ("agy-g38flash", BUNDLES["agy-g38flash"])]:
        ctxf = generate.CTX_FILE[tool]
        jobs += [(f"{name}-A", tool, model, "A"), (f"{name}-B", tool, model, "B"), (f"{name}-C-{ctxf}", tool, model, "C", ctxf)]
    if a.only: jobs = [j for j in jobs if j[0].startswith(tuple(a.only.split(",")))]
    def one(*j):
        r = canary.one(*j)
        if j[3] == "B" and j[1] == "claude":          # E6 addition: the factory mods must not be loaded (managed-settings override worked)
            r["factory_mods_loaded"] = [n for n in (r.get("plugins_loaded") or []) if n in FACTORY_MODS]   # limitation, see header; does not flip pass (text-level canary)
        r["run_at"] = time.strftime("%Y-%m-%d %H:%M")
        return r
    out = []
    with cf.ThreadPoolExecutor(2) as ex:
        for f in cf.as_completed([ex.submit(one, *j) for j in jobs]):
            try: r = f.result()
            except Exception as e: r = {"error": repr(e)}
            out.append(r); print(json.dumps(r)[:300], flush=True)
    old = {r["name"]: r for r in json.loads((RES / "canary.json").read_text())["runs"]} if a.only and (RES / "canary.json").exists() else {}
    old.update({r.get("name", "?"): r for r in out})
    (RES / "canary.json").write_text(json.dumps({"date": time.strftime("%Y-%m-%d %H:%M"), "runs": sorted(old.values(), key=lambda x: x.get("name", ""))}, indent=1))
    print("canary pass:", sum(1 for r in old.values() if r.get("pass")), "of", len(old))

# ---------------------------------------------------------------- generate
def gen_one(c):
    b, arm, rep = c["bundle"], c["arm"], c["rep"]
    tool, model = BUNDLES[b]
    run = c["run_id"]
    out = RUNS / run
    if (out / "meta.json").exists(): return json.loads((out / "meta.json").read_text())
    if shutil.disk_usage(WORK).free < 5 << 30: raise SystemExit("work disk < 5 GB free: abort")
    pfile, with_ctx = ARMS[arm]
    prompt = (E6 / "prompts" / pfile).read_text()
    ctx = generate.CTX_FILE[tool] if with_ctx else None
    attempts = []
    for attempt in (1, 2):
        wd, raw = H / "corpus" / run, H / "corpus-raw" / (run if attempt == 1 else run + ".a2")
        shutil.rmtree(wd, ignore_errors=True); shutil.rmtree(raw, ignore_errors=True); wd.mkdir(parents=True)
        gsha = None
        if ctx:
            gsha = generate.guardrail_sha(); shutil.copyfile(generate.GUARDRAIL, wd / ctx)
        meta = gen_launch.run_generator(tool, wd, prompt, out_dir=raw, model=model, timeout=TIMEOUT_S)
        files = list(generate.sources(wd, ctx))
        kts = [f for f in files if f.suffix == ".kt"]; gradle = [f for f in files if f.name.startswith("build.gradle")]
        stderr_txt = generate.read_gz(raw / "stderr.txt.gz"); t_all = generate.read_gz(raw / "transcript.jsonl.gz")
        denied = generate.denied_actions(t_all)
        rate = bool(generate.INFRA_RE.search(stderr_txt + " ".join(meta.get("stderr_head") or []) + t_all[-3000:]))
        infra = (not files) and (rate or denied > 0)
        attempts.append({"attempt": attempt, "files": len(files), "exit_code": meta["exit_code"], "infra_failure": infra, "denied_actions": denied})
        if infra and attempt == 1:
            if not denied: time.sleep(300)
            continue
        break
    trans = generate.read_gz(raw / "transcript.jsonl.gz")
    net_hits, n_cmds = generate.net_audit(trans)
    wc, wn = generate.web_audit(trans)
    meta.update({"run_id": run, "bundle": b, "arm": arm, "rep": rep, "tier": c["tier"], "files": len(files), "kotlin_loc": generate.kt_loc(wd), "valid": bool(kts and gradle),
                 "context_file": ctx, "guardrail_sha256": gsha, "attempts": attempts, "net_audit": net_hits, "n_commands": n_cmds, "denied_actions": denied,
                 "web_tool_calls": wc, "web_tools": ",".join(wn), "generated_at": time.strftime("%Y-%m-%dT%H:%M:%S%z"),
                 "invalid_reason": "" if (kts and gradle) else ("agy headless denied a command before any file was written" if denied and not files else "no .kt or no gradle build file")})
    (out / "src").mkdir(parents=True, exist_ok=True)
    for rel in files:
        dst = out / "src" / rel; dst.parent.mkdir(parents=True, exist_ok=True); shutil.copyfile(wd / rel, dst)
    for name in ("transcript.jsonl.gz", "stderr.txt.gz", "transcript_full.jsonl.gz"):
        if (raw / name).exists(): shutil.copyfile(raw / name, out / name)
    (out / "meta.json").write_text(json.dumps(meta, indent=2))
    return meta

def cmd_gen(a):
    todo = [c for c in tier_cells(a.tier) if not (RUNS / c["run_id"] / "meta.json").exists()]
    print(len(todo), "to generate,", a.workers, "workers", flush=True)
    with cf.ThreadPoolExecutor(a.workers) as ex:
        futs = {ex.submit(gen_one, c): c for c in todo}
        for f in cf.as_completed(futs):
            c = futs[f]
            try: m = f.result(); print("done", m["run_id"], "valid" if m["valid"] else "INVALID", m["duration_s"], "s", m.get("cost_usd"), flush=True)
            except Exception as e: print("ERR", c["run_id"], repr(e), flush=True)

# ---------------------------------------------------------------- build (E1 build_run.py, R7, as is)
def cmd_build(a):
    build_run.E1 = H                          # build_run reads/writes <E1>/runs/<id>/{meta.json,src,build.json,...} and <E1>/results/repairs.csv
    (H / "results").mkdir(exist_ok=True)
    cells = tier_cells(a.tier)
    while True:
        todo = [c for c in cells if (RUNS / c["run_id"] / "meta.json").exists() and not (RUNS / c["run_id"] / "build.json").exists()]
        if not todo:
            if a.follow and any(not (RUNS / c["run_id"] / "meta.json").exists() for c in cells): time.sleep(30); continue
            break
        c = todo[0]; run = c["run_id"]
        old = sys.argv
        sys.argv = ["build_run.py", run]
        try:
            with gen_launch.bench_shared():
                build_run.main()
        except Exception as e:
            (RUNS / run / "build.json").write_text(json.dumps({"build": "harness-error", "build_error_first_line": repr(e)[:200]})); print(run, "harness-error", repr(e)[:150], flush=True)
        finally:
            sys.argv = old
    print("BUILD DONE", flush=True)

# ---------------------------------------------------------------- emulator
def adb(*args, timeout=90, binary=False):
    r = subprocess.run([ADB, "-s", SERIAL, *args], capture_output=True, timeout=timeout)
    return r.stdout if binary else r.stdout.decode(errors="replace") + r.stderr.decode(errors="replace")

def emu_up():
    subprocess.run([ADB, "start-server"], capture_output=True)
    log = open(H / "emulator.log", "a")
    p = subprocess.Popen(["systemd-run", "--user", "--scope", "-p", "MemorySwapMax=0", EMU, *EMU_FLAGS], stdout=log, stderr=log, stdin=subprocess.DEVNULL,
                         env={**os.environ, "ANDROID_SDK_ROOT": str(pathlib.Path.home() / "Android/Sdk"), "ANDROID_AVD_HOME": str(AVD_HOME)}, start_new_session=True)
    t0 = time.time()
    while time.time() - t0 < 420:
        if "device" in subprocess.run([ADB, "-s", SERIAL, "get-state"], capture_output=True, text=True).stdout and adb("shell", "getprop", "sys.boot_completed", timeout=15).strip() == "1":
            time.sleep(8); return p
        time.sleep(5)
    p.terminate(); raise SystemExit("emulator did not boot in 420 s")

def emu_down(p):
    subprocess.run([ADB, "-s", SERIAL, "emu", "kill"], capture_output=True)
    try: p.wait(30)
    except Exception: subprocess.run(["pkill", "-f", f"qemu-system.*-port {PORT}"])

class exclusive_bench:
    """Exclusive bench.lock (generators/builds hold it shared) + bench.hold so peers start nothing new while the emulator measures."""
    def __enter__(self):
        (WORK / "bench.hold").write_text(f"{os.getpid()} e6-measure\n")
        tf = open(WORK / "bench.turnstile", "a"); fcntl.flock(tf, fcntl.LOCK_EX)
        self.lk = open(WORK / "bench.lock", "a"); fcntl.flock(self.lk, fcntl.LOCK_EX); fcntl.flock(tf, fcntl.LOCK_UN); tf.close()
    def __exit__(self, *x):
        fcntl.flock(self.lk, fcntl.LOCK_UN); self.lk.close(); (WORK / "bench.hold").unlink(missing_ok=True)

def shot(path_png):
    data = adb("exec-out", "screencap", "-p", binary=True)
    path_png.write_bytes(data)

def crashed(pkg):
    log = adb("logcat", "-d", "-b", "crash")
    fatal = re.search(r"FATAL EXCEPTION.*?\n.*?Process: " + re.escape(pkg), log, re.S) is not None
    alive = bool(adb("shell", "pidof", pkg).strip())
    return fatal or not alive, fatal

def measure_one(run):
    d = RUNS / run
    apk = WORK / "apk-corpus" / f"{run}.apk"
    m = {"installed": "no", "package": "", "apk_mb": ""}
    if not apk.exists():
        return m
    m["apk_mb"] = round(apk.stat().st_size / 1e6, 2)
    pkg = subprocess.run([APKANALYZER, "manifest", "application-id", str(apk)], capture_output=True, text=True).stdout.strip()
    m["package"] = pkg
    adb("uninstall", pkg); inst = adb("install", "-r", "-t", str(apk), timeout=180)
    if "Success" not in inst:
        m["install_error"] = inst.strip()[-200:]; return m
    m["installed"] = "yes"
    comp = adb("shell", "cmd", "package", "resolve-activity", "--brief", "-c", "android.intent.category.LAUNCHER", pkg).strip().splitlines()[-1].strip()
    m["component"] = comp
    adb("shell", "pm", "grant", pkg, "android.permission.POST_NOTIFICATIONS")
    for scale, tag in (("1.0", "f1"), ("2.0", "f2")):
        adb("shell", "settings", "put", "system", "font_scale", scale)
        adb("shell", "am", "force-stop", pkg); adb("logcat", "-c")
        out = adb("shell", "am", "start", "-W", "-n", comp)
        mt = re.search(r"TotalTime:\s*(\d+)", out)
        if tag == "f1": m["launch_ms"] = int(mt.group(1)) if mt else ""; m["start_status"] = (re.search(r"Status:\s*(\w+)", out) or [None, ""])[1]
        time.sleep(5)
        dead, fatal = crashed(pkg)
        m[f"{tag}_crash"] = "yes" if dead else "no"; m[f"{tag}_fatal"] = "yes" if fatal else "no"
        shot(d / f"{tag}.png")
        if tag == "f1" and not dead:
            adb("shell", "dumpsys", "gfxinfo", pkg, "reset")
            swipes = "; ".join((["input swipe 360 1000 360 350 300; sleep 0.7"] if i % 2 == 0 else ["input swipe 360 350 360 1000 300; sleep 0.7"])[0] for i in range(12))
            adb("shell", swipes, timeout=120); time.sleep(1.5)
            g = adb("shell", "dumpsys", "gfxinfo", pkg)
            tf_ = re.search(r"Total frames rendered:\s*(\d+)", g); jk = re.search(r"Janky frames:\s*(\d+)\s*\(([\d.]+)%\)", g)
            m["frames_total"] = int(tf_.group(1)) if tf_ else ""; m["janky_frames"] = int(jk.group(1)) if jk else ""; m["janky_pct"] = float(jk.group(2)) if jk else ""
            for k, pat in (("p50_ms", r"50th percentile:\s*(\d+)ms"), ("p90_ms", r"90th percentile:\s*(\d+)ms"), ("p99_ms", r"99th percentile:\s*(\d+)ms")):
                x = re.search(pat, g); m[k] = int(x.group(1)) if x else ""
            fs = adb("shell", "dumpsys", "gfxinfo", pkg, "framestats")           # last <=120 frames: total frame time = FrameCompleted - IntendedVsync
            durs, idx = [], None
            for ln in fs.splitlines():                                           # header-driven: API 34 has an extra FrameTimelineVsyncId column
                c = [x.strip() for x in ln.split(",")]
                if c and c[0] == "Flags": idx = (c.index("IntendedVsync"), c.index("FrameCompleted")) if "FrameCompleted" in c else None; continue
                if idx and len(c) > idx[1] and c[0] == "0" and c[idx[0]].isdigit() and c[idx[1]].isdigit() and int(c[idx[1]]) > int(c[idx[0]]): durs.append((int(c[idx[1]]) - int(c[idx[0]])) / 1e6)
            durs.sort(); m["fs_frames"] = len(durs)
            m["frame_ms_med"] = round(durs[len(durs) // 2], 1) if durs else ""; m["frame_ms_p90"] = round(durs[int(len(durs) * 0.9)], 1) if durs else ""
            m["no_scroll"] = "yes" if (m["frames_total"] == "" or m["frames_total"] < 30) else "no"
            mem = adb("shell", "dumpsys", "meminfo", pkg)
            x = re.search(r"TOTAL PSS:\s*(\d+)", mem) or re.search(r"^\s*TOTAL\s+(\d+)", mem, re.M)
            m["pss_mb"] = round(int(x.group(1)) / 1024, 1) if x else ""
            dead2, fatal2 = crashed(pkg)
            m["crash_during_scroll"] = "yes" if dead2 else "no"
    adb("shell", "settings", "put", "system", "font_scale", "1.0")
    adb("uninstall", pkg)
    return m

def cmd_measure(a):
    cells = [c for c in tier_cells(a.tier) if (RUNS / c["run_id"] / "build.json").exists() and not (RUNS / c["run_id"] / "measure.json").exists()
             and json.loads((RUNS / c["run_id"] / "build.json").read_text()).get("build") == "pass"]
    print(len(cells), "to measure", flush=True)
    if not cells: return
    with exclusive_bench():
        p = emu_up()
        try:
            for c in cells:
                run = c["run_id"]
                try:
                    m = measure_one(run)
                except Exception as e:
                    m = {"measure_error": repr(e)[:200]}
                    if "device" in repr(e) or not adb("get-state").strip():   # emulator lost: restart it once and retry this run
                        emu_down(p); p = emu_up()
                        try: m = measure_one(run)
                        except Exception as e2: m = {"measure_error": repr(e2)[:200]}
                m["measured_at"] = time.strftime("%Y-%m-%dT%H:%M:%S%z")
                (RUNS / run / "measure.json").write_text(json.dumps(m, indent=1)); print(run, {k: m.get(k) for k in ("f1_crash", "launch_ms", "janky_pct", "frames_total", "pss_mb")}, flush=True)
        finally:
            emu_down(p)

# ---------------------------------------------------------------- detector
def cmd_scan(a):
    for c in tier_cells(a.tier):
        run = c["run_id"]; d = RUNS / run
        if not (d / "meta.json").exists() or (d / "scan.json").exists(): continue
        if not json.loads((d / "meta.json").read_text()).get("valid"): continue
        r = scan_corpus.scan_one(d / "src", run)
        lf = d / "lint_findings.json"; r["lint"] = json.loads(lf.read_text()) if lf.exists() else []
        (d / "scan.json").write_text(json.dumps(r)); print("scanned", run, len(r["findings"]) + len(r["detekt"]) + len(r["lint"]), flush=True)

# ---------------------------------------------------------------- collect
def med(xs):
    xs = [x for x in xs if x != "" and x is not None]
    return round(statistics.median(xs), 2) if xs else ""

def cmd_collect(a):
    from PIL import Image, ImageDraw, ImageFont
    RES.mkdir(exist_ok=True); IMG.mkdir(exist_ok=True)
    rows, find_results = [], []
    for c in load_plan():
        run = c["run_id"]; d = RUNS / run
        row = {"run_id": run, "order": c["order"], "tier": c["tier"], "bundle": c["bundle"], "arm": c["arm"], "rep": c["rep"]}
        meta = json.loads((d / "meta.json").read_text()) if (d / "meta.json").exists() else None
        build = json.loads((d / "build.json").read_text()) if (d / "build.json").exists() else None
        ms = json.loads((d / "measure.json").read_text()) if (d / "measure.json").exists() else None
        scan = json.loads((d / "scan.json").read_text()) if (d / "scan.json").exists() else None
        reason = []
        if not meta: reason.append("not generated")
        else:
            row.update({k: meta.get(k, "") for k in ("tool", "model_arg", "model_reported", "cli_version", "generated_at", "duration_s", "tokens_in", "tokens_out", "cost_usd", "exit_code", "timeout", "files", "kotlin_loc", "valid", "net_audit", "web_tool_calls", "denied_actions", "harness_config_hash", "context_file", "guardrail_sha256", "prompt_sha256")})
            if not meta.get("valid"): reason.append("invalid: " + meta.get("invalid_reason", ""))
        if build:
            row.update({"build": build.get("build"), "build_zero_touch": build.get("build_zero_touch", ""), "repairs": build.get("repairs", ""), "build_error_first_line": (build.get("build_error_first_line") or "")[:200], "lint_issues": build.get("lint_issues", "")})
            if build.get("build") != "pass": reason.append("build: " + (build.get("build_error_first_line") or build.get("build") or "")[:120])
        if ms:
            row.update({k: ms.get(k, "") for k in ("package", "apk_mb", "installed", "launch_ms", "start_status", "f1_crash", "f1_fatal", "f2_crash", "f2_fatal", "frames_total", "janky_frames", "janky_pct", "p50_ms", "p90_ms", "p99_ms", "fs_frames", "frame_ms_med", "frame_ms_p90", "no_scroll", "pss_mb", "crash_during_scroll", "measure_error")})
            if ms.get("installed") != "yes" and ms.get("package") != "": reason.append("install failed")
        if scan:
            cnt = {"S": 0, "P": 0, "U": 0, "D": 0, "other": 0}; lint_other = 0
            for f in scan["findings"] + scan["detekt"] + scan["lint"]:
                k = f["rule_id"][:1] if re.match(r"^[SPUD]\d", f["rule_id"]) else "other"
                cnt[k] += 1
                if f["rule_id"].startswith("lint:"): lint_other += 1
            row.update({"S": cnt["S"], "P": cnt["P"], "U": cnt["U"], "D": cnt["D"], "other_findings": cnt["other"], "findings_total": sum(cnt.values())})
            find_results.append(scan)
        row["failure_reason"] = "; ".join(reason)
        for tag in ("f1", "f2"):
            png = d / f"{tag}.png"
            if png.exists():
                im = Image.open(png).convert("RGB"); im = im.resize((540, int(im.height * 540 / im.width)))
                im.save(IMG / f"{run}-{tag}.jpg", quality=78); row[f"img_{tag}"] = f"img/{run}-{tag}.jpg"
        rows.append(row)
        for fn in ("meta.json", "build.json", "measure.json"):                  # small provenance copies into the repo
            if (d / fn).exists():
                (E6 / "runs" / run).mkdir(parents=True, exist_ok=True); shutil.copyfile(d / fn, E6 / "runs" / run / fn)
    cols = ["run_id", "order", "tier", "bundle", "arm", "rep", "tool", "model_arg", "model_reported", "cli_version", "generated_at", "duration_s", "tokens_in", "tokens_out", "cost_usd", "exit_code", "timeout",
            "files", "kotlin_loc", "valid", "net_audit", "web_tool_calls", "denied_actions", "build", "build_zero_touch", "repairs", "build_error_first_line", "lint_issues",
            "package", "apk_mb", "installed", "launch_ms", "start_status", "f1_crash", "f1_fatal", "f2_crash", "f2_fatal", "frames_total", "janky_frames", "janky_pct", "p50_ms", "p90_ms", "p99_ms", "fs_frames", "frame_ms_med", "frame_ms_p90", "no_scroll", "pss_mb", "crash_during_scroll", "measure_error",
            "S", "P", "U", "D", "other_findings", "findings_total", "img_f1", "img_f2", "context_file", "guardrail_sha256", "prompt_sha256", "harness_config_hash", "failure_reason"]
    with open(RES / "runs.csv", "w", newline="") as f:
        w = csv.DictWriter(f, cols, extrasaction="ignore"); w.writeheader(); w.writerows(rows)
    if find_results: scan_corpus.write_all(find_results, RES / "findings.csv", RES / "metrics.csv")
    if (H / "results" / "repairs.csv").exists(): shutil.copyfile(H / "results" / "repairs.csv", RES / "repairs.csv")
    summarize(rows)
    grids(rows, Image, ImageDraw, ImageFont)
    print(len(rows), "rows,", sum(1 for r in rows if r.get("failure_reason")), "with a failure reason")

def num(x):
    try: return float(x)
    except (TypeError, ValueError): return None

def summarize(rows):
    out = []
    for b in BUNDLES:
        for arm in ARMS:
            g = [r for r in rows if r["bundle"] == b and r["arm"] == arm]
            def n_(f): return sum(1 for r in g if f(r))
            launched = [r for r in g if r.get("f1_crash") == "no"]
            jk = [num(r.get("janky_pct")) for r in launched if r.get("no_scroll") == "no" and num(r.get("janky_pct")) is not None]
            def rng(key, src=g):
                v = [num(r.get(key)) for r in src if num(r.get(key)) is not None]
                return f"{min(v):g}-{max(v):g}" if v else ""
            out.append({"bundle": b, "arm": arm, "n_planned": len(g), "n_generated": n_(lambda r: r.get("generated_at")), "n_valid": n_(lambda r: str(r.get("valid")) == "True"),
                        "n_build_pass": n_(lambda r: r.get("build") == "pass"), "n_zero_touch": n_(lambda r: r.get("build_zero_touch") == "pass"),
                        "n_launch_ok": len(launched), "n_scroll_measured": len(jk),
                        "median_kotlin_loc": med([num(r.get("kotlin_loc")) for r in g]), "range_kotlin_loc": rng("kotlin_loc"),
                        "median_janky_pct": med(jk), "range_janky_pct": f"{min(jk):g}-{max(jk):g}" if jk else "", "median_frame_ms": med([num(r.get("frame_ms_med")) for r in launched if r.get("no_scroll") == "no"]), "range_frame_ms": rng("frame_ms_med", [r for r in launched if r.get("no_scroll") == "no"]),
                        "median_launch_ms": med([num(r.get("launch_ms")) for r in launched]), "median_pss_mb": med([num(r.get("pss_mb")) for r in launched]),
                        "median_S": med([num(r.get("S")) for r in g]), "range_S": rng("S"), "median_P": med([num(r.get("P")) for r in g]), "range_P": rng("P"),
                        "median_U": med([num(r.get("U")) for r in g]), "range_U": rng("U"), "median_findings_total": med([num(r.get("findings_total")) for r in g]), "range_findings_total": rng("findings_total"),
                        "median_cost_usd": med([num(r.get("cost_usd")) for r in g]), "median_duration_s": med([num(r.get("duration_s")) for r in g])})
    with open(RES / "summary.csv", "w", newline="") as f:
        w = csv.DictWriter(f, list(out[0])); w.writeheader(); w.writerows(out)

def best_rep(rows, b, arm):
    """Pre-registered rule (AMENDMENT 6.5): among the cell's reps that launched without crash, lowest janky %, ties by lower median frame ms, then lower rep."""
    g = [r for r in rows if r["bundle"] == b and r["arm"] == arm and r.get("f1_crash") == "no" and r.get("img_f1")]
    if not g: return None
    return sorted(g, key=lambda r: (num(r.get("janky_pct")) if num(r.get("janky_pct")) is not None else 1e9, num(r.get("frame_ms_med")) if num(r.get("frame_ms_med")) is not None else 1e9, r["rep"]))[0]

def font(Image, ImageFont, size):
    for p in ("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", "/usr/share/fonts/dejavu/DejaVuSans.ttf", "/usr/share/fonts/TTF/DejaVuSans.ttf"):
        if os.path.exists(p): return ImageFont.truetype(p, size)
    return ImageFont.load_default()

def grids(rows, Image, ImageDraw, ImageFont, w=360):
    f_big, f_sm = font(Image, ImageFont, 22), font(Image, ImageFont, 16)
    def sheet(arms, name, key="img_f1"):
        cols = list(BUNDLES); h = int(w * 2280 / 1080); pad, head, lab = 12, 70, 52
        W_ = pad + len(cols) * (w + pad); H_ = head + len(arms) * (h + lab + pad)
        im = Image.new("RGB", (W_, H_), "white"); dr = ImageDraw.Draw(im)
        for i, b in enumerate(cols): dr.text((pad + i * (w + pad), 10), b, fill="black", font=f_big); dr.text((pad + i * (w + pad), 38), f"{BUNDLES[b][0]} / {BUNDLES[b][1]}", fill="#555", font=f_sm)
        for j, arm in enumerate(arms):
            y = head + j * (h + lab + pad)
            for i, b in enumerate(cols):
                x = pad + i * (w + pad); r = best_rep(rows, b, arm)
                if r and r.get(key):
                    t = Image.open(E6 / r[key]).resize((w, h)); im.paste(t, (x, y + lab))
                    dr.text((x, y), f"arm {arm} rep {r['rep']}", fill="black", font=f_big)
                    dr.text((x, y + 26), f"jank {r.get('janky_pct') or 'n/a'}%  loc {r.get('kotlin_loc')}  find {r.get('findings_total')}", fill="#333", font=f_sm)
                else:
                    g = [x_ for x_ in rows if x_["bundle"] == b and x_["arm"] == arm]
                    why = next((x_["failure_reason"] for x_ in g if x_.get("failure_reason")), "no run")
                    dr.rectangle([x, y + lab, x + w, y + lab + h], outline="#999", fill="#f1f1f1")
                    dr.text((x, y), f"arm {arm}: no screenshot", fill="#a00", font=f_big)
                    n_b = sum(1 for x_ in g if x_.get("build") == "pass")
                    dr.text((x + 10, y + lab + 10), f"{n_b}/{len(g)} built", fill="#a00", font=f_sm)
                    for k, line in enumerate([why[i2:i2 + 40] for i2 in range(0, min(len(why), 240), 40)]): dr.text((x + 10, y + lab + 34 + 20 * k), line, fill="#333", font=f_sm)
        im.convert("RGB").save(IMG / name, quality=85)
    sheet(["O"], "grid-O.jpg"); sheet(["O", "R", "S"], "grid-ORS.jpg"); sheet(["O"], "grid-O-font2.jpg", "img_f2")   # same reps as grid-O, system font scale 2.0

def main():
    ap = argparse.ArgumentParser(); ap.add_argument("cmd", choices=["canary", "plan", "gen", "build", "measure", "scan", "collect"])
    ap.add_argument("--tier", type=int, default=0); ap.add_argument("--workers", type=int, default=2); ap.add_argument("--follow", action="store_true"); ap.add_argument("--only")
    a = ap.parse_args()
    RUNS.mkdir(parents=True, exist_ok=True); RES.mkdir(parents=True, exist_ok=True)
    {"canary": cmd_canary, "plan": lambda a: (save_plan(), print(len(plan()), "cells")), "gen": cmd_gen, "build": cmd_build, "measure": cmd_measure, "scan": cmd_scan, "collect": cmd_collect}[a.cmd](a)

if __name__ == "__main__":
    main()
