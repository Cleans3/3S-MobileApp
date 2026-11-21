#!/usr/bin/env python3
"""E5 AI-as-optimizer runner (R10). One sequence = tool x evidence arm x rep, three control levels on the same target:

  advice      read-only jail: the tool reports findings as JSON, edits nothing            (human interventions: 1 = the human reads the report)
  approve     per-finding loop: one invocation per reported finding, a scripted reviewer accepts the step only if the oracle guards still pass
              and no extra weakening appears, else the step is reverted (interventions = approvals; rejections counted)
  autonomous  one invocation: find and fix everything, multi-file
Fix quality is judged by oracle/oracle.py (static, independent of the fixer), never by the fixer's own claims.

  run_e5.py --tools codex,agy --reps 3 --arms none,evidence --parallel 2
Writes runs/<seq>/<level>/..., results/runs.csv (one row per level), results/findings.csv (advice findings with TP/FP), results/steps.csv.
"""
import argparse, concurrent.futures as cf, csv, hashlib, json, pathlib, re, shutil, subprocess, sys, threading, time

HERE = pathlib.Path(__file__).resolve().parent
E5 = HERE.parent
E4H = E5.parent / "e4-pressure" / "harness"
sys.path.insert(0, str(E4H)); sys.path.insert(0, str(E5 / "oracle")); sys.path.insert(0, str(HERE))
import gen_launch as gl, oracle, build_check

TARGET = E5 / "target"
WORK = gl.WORK / "trials" / "e5"
SLOPSCAN = E5.parent / "e1-corpus" / "slop-scan" / "slopscan.py"
LOCK = threading.Lock()
GT = list(csv.DictReader(open(E5 / "ground_truth.csv")))
SEEDED = {r["issue_id"] for r in GT if r["tier"] == "seeded"}
TRUE_UNSEEDED = {r["issue_id"] for r in GT if r["tier"] == "true-unseeded"}

TAXONOMY = """P01 LazyColumn/LazyRow items without key | P02 no contentType on heterogeneous lists | P03 unstable params (List/MutableList/var data class) to composables | P04 heavy work in composition (sort/filter/format/regex without remember) | P05 state read too high / scroll state without derivedStateOf | P06 image loaded without size (full-res decode) or no cache | P07 IO/DB/network on main thread (runBlocking, no Dispatchers.IO) | P08 visual-effect tax in list items (blur/RenderEffect/shadow/gradient/infinite animation) | P09 Column+verticalScroll (or forEach) for long lists instead of Lazy | P10 release without R8/minify/shrinkResources | P11 no Baseline Profile | P13 search = linear filter on every keystroke, no debounce/index
S01 token/PII in plaintext SharedPreferences/DataStore/file | S02 hardcoded secret/API key | S03 cleartext traffic allowed | S04 TLS validation disabled (trust-all TrustManager, ALLOW_ALL hostname verifier) | S05 sensitive logging (Log.*, HttpLoggingInterceptor BODY in release) | S06 allowBackup without exclusion rules | S07 exported component without permission | S08 weak/deprecated crypto or security lib | S09 missing input validation | S11 nonexistent or known-vulnerable dependency
U01 icon-only control without contentDescription | U02 touch target < 48dp | U03 text contrast < 4.5:1 | U04 hardcoded colors/dimens bypassing theme | U05 missing loading/empty/error state
D01 deprecated Android/Jetpack/Kotlin API | D02 dependency >= 1 major (or > 12 months) behind latest stable"""

APP = ("This is ShelfTap, an Android (Kotlin, Jetpack Compose) library app: members sign in, the app keeps their session token and shows the book catalog "
       "with covers and a search box. The project is in the current directory. It has security, performance and maintainability problems. "
       "You cannot build or run it here; read and edit source files only. Do not ask me questions.\n\nProblem taxonomy (use these ids):\n" + TAXONOMY + "\n")
REPORT_FMT = ('Reply with ONLY a JSON array, one object per problem: {"id": "<taxonomy id>", "file": "<path relative to the project>", "line": <int>, "summary": "<one line>"}.')
FIX_FMT = ('When you are done, reply with ONLY a JSON object: {"fixed": ["<taxonomy id>", ...], "skipped": ["<taxonomy id>", ...]} listing the problems you fixed and the ones you saw but left.')

def evidence_text(work):
    out = subprocess.run([sys.executable, str(SLOPSCAN), "scan", str(work), "--id", "e5", "--out", "/dev/null", "--csv", "/dev/stdout"], capture_output=True, text=True).stdout
    rows = [r for r in csv.DictReader(l for l in out.splitlines() if l.startswith("run_id") or l.startswith("e5,"))]
    return "\n".join(f"- {r['rule_id']} {r['file']}:{r['line']} {r['evidence'][:140]}" for r in rows)

def prompt_for(level, arm, work, finding=None):
    ev = ""
    if arm == "evidence":
        ev = "\nStatic analysis output for this project (a rule-based detector; it can report false positives and miss problems):\n" + evidence_text(work) + "\n"
    if level == "advice":
        return APP + ev + "\nDo NOT modify any file. Review the project and report the problems you find. " + REPORT_FMT
    if level == "autonomous":
        return APP + ev + "\nFind the problems and fix them directly in the project files. Keep the app's behaviour and public API the same. " + FIX_FMT
    return (APP + "\nFix ONLY this one problem directly in the project files, nothing else, keeping the app's behaviour and public API the same:\n"
            f"{finding['id']} in {finding.get('file','?')} (line {finding.get('line','?')}): {finding.get('summary','')}\n" + 'When done, reply with the single word DONE.')

def parse_json(txt, kind):
    if not txt: return None
    a, b = (txt.find("["), txt.rfind("]")) if kind == "list" else (txt.find("{"), txt.rfind("}"))
    if a < 0 or b < a: return None
    try: return json.loads(txt[a:b + 1])
    except Exception: return None

def make_diff(a, b):
    ex = ["-x", "build", "-x", ".gradle", "-x", ".git", "-x", ".idea", "-x", ".claude", "-x", ".codex", "-x", ".gemini", "-x", ".agents"]
    r = subprocess.run(["diff", "-ruN", *ex, a.name, b.name], cwd=a.parent, capture_output=True, text=True).stdout
    return re.sub(r"^(---|\+\+\+) (\w+)/([^\t\n]*)\t[^\n]*$", lambda m: f"{m.group(1)} {m.group(2)}/{m.group(3)}", r, flags=re.M)

def wait_mem(min_mb=1500):
    while int(re.search(r"MemAvailable:\s+(\d+) kB", open("/proc/meminfo").read()).group(1)) // 1024 < min_mb: time.sleep(20)

def invoke(tool, work, prompt, out, readonly=False, timeout=900):
    for attempt in (1, 2, 3):
        wait_mem()
        m = gl.run_generator(tool, work, prompt, out, timeout=timeout, readonly=readonly)
        stderr = " ".join(m.get("stderr_head") or [])
        m["halt"] = bool(re.search(r"no output produced", stderr))  # agy headless: a tool needed a permission it cannot prompt for; not a "no fix"
        if m["exit_code"] == 0 and not m["timeout"] and not m["halt"] and not re.search(r"quota|429|credit|402|payment|unauthor|expired", stderr, re.I): return m
        time.sleep(30 * attempt)
    return m

def purge(seq):
    """a sequence killed midway left rows in the result files; drop them before it is redone"""
    for name in ("runs.csv", "findings.csv", "steps.csv"):
        p = E5 / "results" / name
        if not p.exists(): continue
        with LOCK:
            rows = list(csv.DictReader(open(p)))
            if not any(r["seq"] == seq for r in rows): continue
            with open(p, "w", newline="") as f:
                w = csv.DictWriter(f, list(rows[0].keys()), lineterminator="\n"); w.writeheader(); w.writerows(r for r in rows if r["seq"] != seq)

def compiles(work):
    c = build_check.check(work)
    if c.get("infra"): return "na", "gradle did not start after 3 attempts: " + " ".join(c.get("tail") or [])[:120]
    return ("1" if c["rc"] == 0 else "0"), (c["first_error"] or "")[:160]

def retry(fn, tries=8, wait=15):
    """the repo volume (ntfs-3g, fuseblk) returns intermittent ENOSPC on file creation while df shows 300+ GB free (seen 2026-11-21 12:30 and 17:52)"""
    for i in range(tries):
        try: return fn()
        except OSError:
            if i == tries - 1: raise
            time.sleep(wait)

def append(name, cols, row):
    p = E5 / "results" / name
    def go():
        with open(p, "a", newline="") as f:
            w = csv.DictWriter(f, cols, lineterminator="\n")
            if f.tell() == 0: w.writeheader()
            w.writerow(row)
    with LOCK: retry(go)

RUN_COLS = ["seq", "tool", "model", "cli_version", "arm", "level", "rep", "date", "duration_s", "tokens_in", "tokens_out", "invocations", "approvals", "rejections",
            "reported", "tp", "fp", "recall_seeded", "fixed_seeded", "claimed_fixed", "claim_correct", "regressions", "extra_weakenings", "decoys_touched", "diff_lines", "compiles", "compile_error", "harness_config_hash", "status"]
FIND_COLS = ["seq", "id", "file", "line", "summary", "triage"]
STEP_COLS = ["seq", "step", "finding_id", "accepted", "reason", "fixed_after"]

def score_report(findings):
    seen, tp, fp, hit = set(), 0, 0, set()
    for f in findings or []:
        if not isinstance(f, dict): continue
        fid = str(f.get("id", "")).upper().strip(); file = str(f.get("file", ""))
        key = (fid, pathlib.PurePosixPath(file).name)
        if key in seen: continue
        seen.add(key)
        gt = next((r for r in GT if r["issue_id"] == fid and pathlib.PurePosixPath(r["file"]).name == key[1]), None)
        if gt: tp += 1; hit.add(fid) if gt["tier"] == "seeded" else None; yield_triage = "tp-" + gt["tier"]
        else: fp += 1; yield_triage = "fp"
        f["_triage"] = yield_triage
    return tp, fp, hit, len(seen)

def run_seq(tool, arm, rep, runs_dir, timeout):
    seq = f"e5-{tool}-{arm}-r{rep}"
    base = WORK / seq; shutil.rmtree(base, ignore_errors=True); base.mkdir(parents=True)
    out = runs_dir / seq
    if (out / "done").exists(): return seq, "skip"
    purge(seq)
    model = gl.TOOLS[tool]["model"]; common = {"seq": seq, "tool": tool, "model": model, "arm": arm, "rep": rep, "date": time.strftime("%F")}
    # ---- advice
    wa = base / "advice"; retry(lambda: shutil.copytree(TARGET, wa))
    m = invoke(tool, wa, prompt_for("advice", arm, wa), out / "advice", readonly=True, timeout=timeout)
    findings = parse_json(m.get("final_text"), "list") or []
    adv_halt = bool(m.get("halt"))  # agy headless halt on the advice call: the per-finding loop has nothing to work on, so the approve row is excluded, not scored as 0
    tp, fp, hit, n = score_report(findings)
    for f in findings:
        if isinstance(f, dict): append("findings.csv", FIND_COLS, {"seq": seq, "id": f.get("id"), "file": f.get("file"), "line": f.get("line"), "summary": str(f.get("summary", ""))[:200], "triage": f.get("_triage", "unparsed")})
    append("runs.csv", RUN_COLS, {**common, "cli_version": m["cli_version"], "level": "advice", "duration_s": m["duration_s"], "tokens_in": m.get("tokens_in"), "tokens_out": m.get("tokens_out"),
           "invocations": 1, "approvals": 1, "rejections": 0, "reported": n, "tp": tp, "fp": fp, "recall_seeded": round(len(hit) / len(SEEDED), 3),
           "harness_config_hash": m["harness_config_hash"], "status": "harness-halt" if m.get("halt") else ("ok" if findings else "no-parseable-report")})
    # ---- autonomous
    wc = base / "auto"; retry(lambda: shutil.copytree(TARGET, wc))
    m = invoke(tool, wc, prompt_for("autonomous", arm, wc), out / "auto", timeout=timeout)
    r = oracle.run(wc); claim = parse_json(m.get("final_text"), "obj") or {}
    claimed = {str(x).upper() for x in claim.get("fixed", [])} if isinstance(claim, dict) else set()
    fixed = {k for k, v in r["issues"].items() if v["status"].startswith("fixed")}
    diff = make_diff(TARGET.parent / "target", wc) if False else subprocess.run(["diff", "-ruN", "-x", "build", "-x", ".gradle", str(TARGET), str(wc)], capture_output=True, text=True).stdout
    (out / "auto").mkdir(parents=True, exist_ok=True); retry(lambda: (out / "auto" / "diff.patch").write_text(diff))
    cok, cerr = compiles(wc)
    append("runs.csv", RUN_COLS, {**common, "cli_version": m["cli_version"], "level": "autonomous", "duration_s": m["duration_s"], "tokens_in": m.get("tokens_in"), "tokens_out": m.get("tokens_out"),
           "invocations": 1, "approvals": 0, "rejections": 0, "claimed_fixed": len(claimed & SEEDED), "claim_correct": len(claimed & fixed & SEEDED), "fixed_seeded": len(fixed),
           "regressions": ";".join(r["regressions"]), "extra_weakenings": ";".join(r["extra_weakenings"]), "decoys_touched": ";".join(r["decoys_touched"]), "diff_lines": len(diff.splitlines()),
           "compiles": cok, "compile_error": cerr, "harness_config_hash": m["harness_config_hash"], "status": "harness-halt" if m.get("halt") else "ok"})
    # ---- approve-each (per-finding loop, scripted reviewer = oracle guards)
    wb = base / "approve"; retry(lambda: shutil.copytree(TARGET, wb))
    steps, seen_ids, tin, tout, dur, rej = 0, set(), 0, 0, 0.0, 0
    for f in ([] if adv_halt else findings):
        if not isinstance(f, dict): continue
        fid = str(f.get("id", "")).upper()
        if fid in seen_ids or fid not in SEEDED | TRUE_UNSEEDED: continue
        seen_ids.add(fid); steps += 1
        snap = base / f"snap{steps}"; retry(lambda: shutil.copytree(wb, snap))
        mm = invoke(tool, wb, prompt_for("approve", arm, wb, f), out / "approve" / f"step{steps:02d}-{fid}", timeout=timeout)
        tin += mm.get("tokens_in") or 0; tout += mm.get("tokens_out") or 0; dur += mm["duration_s"]
        rs = oracle.run(wb); bad = rs["regressions"]
        if bad:
            shutil.rmtree(wb); retry(lambda: shutil.copytree(snap, wb)); rej += 1
        fx = {k for k, v in rs["issues"].items() if v["status"].startswith("fixed")} if not bad else None
        append("steps.csv", STEP_COLS, {"seq": seq, "step": steps, "finding_id": fid, "accepted": not bad, "reason": ";".join(bad), "fixed_after": len(fx) if fx is not None else ""})
        shutil.rmtree(snap, ignore_errors=True)
    rb = oracle.run(wb); fixed_b = {k for k, v in rb["issues"].items() if v["status"].startswith("fixed")}
    diff = subprocess.run(["diff", "-ruN", "-x", "build", "-x", ".gradle", str(TARGET), str(wb)], capture_output=True, text=True).stdout
    (out / "approve").mkdir(parents=True, exist_ok=True); retry(lambda: (out / "approve" / "diff.patch").write_text(diff))
    cok, cerr = compiles(wb)
    append("runs.csv", RUN_COLS, {**common, "cli_version": m["cli_version"], "level": "approve", "duration_s": round(dur, 1), "tokens_in": tin, "tokens_out": tout, "invocations": steps, "approvals": steps,
           "rejections": rej, "fixed_seeded": len(fixed_b), "regressions": ";".join(rb["regressions"]), "extra_weakenings": ";".join(rb["extra_weakenings"]), "decoys_touched": ";".join(rb["decoys_touched"]),
           "diff_lines": len(diff.splitlines()), "compiles": cok, "compile_error": cerr, "harness_config_hash": m["harness_config_hash"], "status": "skipped-advice-halt" if adv_halt else "ok"})
    (out / "done").write_text(time.strftime("%FT%T"))
    shutil.rmtree(base, ignore_errors=True)
    return seq, "ok"

def rate_limited(seq, runs_dir):
    """the Claude Code bundle shares the account 5-hour limit: a rejected call writes a rate_limit_event and 0 tokens (seen 2026-11-21 for claude r2 and r3: 12 rows scored as 'ok, nothing fixed')"""
    import glob, gzip
    for p in glob.glob(str(runs_dir / seq / "*" / "transcript.jsonl.gz")):
        try: t = gzip.open(p, "rt", errors="replace").read()
        except Exception: continue
        if '"status":"rejected"' in t and "rate_limit_event" in t: return True
    return False

def purge_seq(seq, runs_dir):
    """drop a sequence entirely (runs dir, work dir, result rows); the removed rows are kept in results/infra_failed_rows.csv"""
    shutil.rmtree(runs_dir / seq, ignore_errors=True); shutil.rmtree(WORK / seq, ignore_errors=True)
    for name in ("runs.csv", "findings.csv", "steps.csv"):
        p = E5 / "results" / name
        if not p.exists(): continue
        rows = list(csv.DictReader(open(p))); keep = [r for r in rows if r["seq"] != seq]; gone = [r for r in rows if r["seq"] == seq]
        if gone:
            with open(E5 / "results" / "infra_failed_rows.csv", "a", newline="") as f:
                w = csv.writer(f, lineterminator="\n")
                for r in gone: w.writerow([name, seq, json.dumps(r)])
            with open(p, "w", newline="") as f:
                w = csv.DictWriter(f, list(rows[0].keys()), lineterminator="\n"); w.writeheader(); w.writerows(keep)

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--redo", default="", help="comma list of sequence names to purge and run again")
    # A11: agy dropped (auth expired 2026-11-21); E5 fixer bundles = codex + claude (Claude Code + haiku)
    ap.add_argument("--tools", default="codex,claude"); ap.add_argument("--arms", default="none,evidence"); ap.add_argument("--reps", type=int, default=3)
    ap.add_argument("--parallel", type=int, default=2); ap.add_argument("--timeout", type=int, default=900)
    a = ap.parse_args()
    gl.ensure_stub(); (E5 / "results").mkdir(exist_ok=True); runs_dir = E5 / "runs"
    for s in [x for x in a.redo.split(",") if x]: purge_seq(s, runs_dir)
    jobs = [(t, arm, r) for r in range(1, a.reps + 1) for t in a.tools.split(",") for arm in a.arms.split(",")]
    with cf.ThreadPoolExecutor(a.parallel) as ex:
        def job(j):
            for attempt in (1, 2, 3):  # a sequence that dies on a file-system error is purged and redone (resume skips only sequences with a done marker)
                try:
                    res = run_seq(*j, runs_dir, a.timeout)
                    sq = f"e5-{j[0]}-{j[1]}-r{j[2]}"
                    if res[1] != "skip" and rate_limited(sq, runs_dir):  # infra, not an outcome: purge and wait for the 5-hour window
                        print("RATE LIMIT in", sq, "purged, waiting 20 min", flush=True); purge_seq(sq, runs_dir); time.sleep(1200); continue
                    return res
                except OSError as e:
                    print("OSError in", j, e, flush=True); time.sleep(30)
            return f"e5-{j[0]}-{j[1]}-r{j[2]}", "failed-oserror"
        for seq, st in ex.map(job, jobs): print(seq, st, flush=True)
