#!/usr/bin/env python3
"""E4 trial runner. One trial = case x tool x variant x rep.

  run_trials.py --reps 5 [--tools claude,codex,agy] [--cases c1-cleartext,...] [--variants A,B] [--parallel 3] [--pilot]
Writes runs/<trial_id>/{prompt.txt,diff.patch,meta.json,transcript.jsonl.gz,stderr.txt.gz} and appends results/trials_meta.csv
and results/auto_labels.csv. Resumable: a trial with runs/<id>/meta.json is skipped. --pilot: ids prefixed pilot-, rubric not checked,
outputs under runs-pilot/ and results/pilot_*.csv (never mixed into the analysis).
"""
import argparse, concurrent.futures as cf, csv, gzip, hashlib, json, pathlib, re, shutil, subprocess, sys, threading, time

HERE = pathlib.Path(__file__).resolve().parent
E4 = HERE.parent
sys.path.insert(0, str(HERE))
import gen_launch as gl, prompts, classify

WORK = gl.WORK / "trials" / "e4"
CASES = ["c1-cleartext", "c2-selftls", "c3-mainthread", "c4-r8", "c5-keystore"]
LOCK = threading.Lock()
META_COLS = ["trial_id", "case", "tool", "model", "model_reported", "cli_version", "variant", "rep", "date", "duration_s", "tokens_in", "tokens_out",
             "cost_usd", "harness_config_hash", "prompt_sha256", "cli_flags", "exit_code", "files_changed", "diff_lines", "diff_sha256", "net_tool_calls", "attempts", "status"]
AUTO_COLS = ["trial_id", "case", "auto_outcome", "auto_rules", "auto_evidence", "auto_tags"]
INFRA_RE = re.compile(r"quota|429|rate.?limit|unauthor|credit|402|payment|ECONN|ETIMEDOUT|overloaded|503|529|login|expired|no output produced", re.I)

def check_rubric():
    want = (E4 / "RUBRIC.sha256").read_text().strip()
    got = hashlib.sha256((E4 / "RUBRIC.md").read_bytes()).hexdigest()
    if want != got:
        sys.exit(f"RUBRIC.md changed after freeze ({got[:12]} != {want[:12]}); revise via the revision log + new RUBRIC.sha256 before trials")

def tid(case, tool, variant, rep, pilot):
    return ("pilot-" if pilot else "") + f"{case.split('-')[0]}-{tool}-{variant}-r{rep}"

def make_diff(a, b):
    ex = ["-x", "build", "-x", ".gradle", "-x", ".idea", "-x", ".git", "-x", ".gemini", "-x", ".claude", "-x", ".codex", "-x", ".agy*", "-x", ".agents", "-x", "*.orig"]
    r = subprocess.run(["diff", "-ruN", *ex, a.name, b.name], cwd=a.parent, capture_output=True, text=True)
    out = re.sub(r"^(---|\+\+\+) (orig|work)/([^\t\n]*)\t[^\n]*$", lambda m: f"{m.group(1)} {'a' if m.group(2)=='orig' else 'b'}/{m.group(3)}", r.stdout, flags=re.M)
    return out

def net_calls(tool, out_dir):
    """v4: counted by audit_web.py (web-tool calls + network shell commands); the older regex below also matched tool lists in the init event and is kept only for reference."""
    import audit_web
    a = audit_web.audit(out_dir) if (out_dir / "meta.json").exists() else None
    if a: return a["builtin_web"] + a["shell_net"]
    return _net_calls_old(tool, out_dir)

def _net_calls_old(tool, out_dir):
    pats = {"claude": r'"name":"(WebFetch|WebSearch)"', "codex": r"curl |wget |https?://", "agy": r"read_url_content|search_web|browser_"}
    n = 0
    for f in ("transcript.jsonl.gz", "transcript_full.jsonl.gz"):
        p = out_dir / f
        if p.exists():
            txt = gzip.open(p, "rt", errors="replace").read()
            if tool == "codex":  # only count shell commands, not URLs in repo files the model read
                n += len(re.findall(r'"command":"[^"]*(?:curl|wget)[^"]*"', txt))
            else:
                n += len(re.findall(pats[tool], txt))
    return n

def wait_mem(min_mb=1500):
    """The box is shared and nearly out of RAM: do not start a generator while MemAvailable is low."""
    while True:
        avail = int(re.search(r"MemAvailable:\s+(\d+) kB", open("/proc/meminfo").read()).group(1)) // 1024
        if avail >= min_mb: return
        time.sleep(20)

def run_one(case, tool, variant, rep, pilot, runs_dir, timeout, max_attempts=3):
    t = tid(case, tool, variant, rep, pilot)
    out = runs_dir / t
    if (out / "meta.json").exists():
        return t, "skip"
    prompt = prompts.build_prompt(case, variant)
    for attempt in range(1, max_attempts + 1):
        wait_mem()
        base = WORK / t
        shutil.rmtree(base, ignore_errors=True)
        shutil.copytree(prompts.CASES_DIR / case / "project", base / "orig")
        shutil.copytree(base / "orig", base / "work")
        out.mkdir(parents=True, exist_ok=True)
        (out / "prompt.txt").write_text(prompt)
        m = gl.run_generator(tool, base / "work", prompt, out, timeout=timeout)
        diff = make_diff(base / "orig", base / "work")
        stderr_txt = " ".join(m.get("stderr_head") or [])
        infra = (m["exit_code"] != 0 and not diff.strip()) or (m["timeout"] and not diff.strip()) or (not diff.strip() and INFRA_RE.search(stderr_txt))
        if infra and attempt < max_attempts:
            with LOCK:
                p = E4 / "results" / ("pilot_" if pilot else "") / "infra_failures.csv" if False else E4 / "results" / (("pilot_" if pilot else "") + "infra_failures.csv")
                new = not p.exists()
                with open(p, "a", newline="") as f:
                    w = csv.writer(f, lineterminator="\n")
                    if new: w.writerow(["trial_id", "attempt", "date", "tool", "exit_code", "timeout", "stderr_head_verbatim"])
                    w.writerow([t, attempt, time.strftime("%FT%T"), tool, m["exit_code"], m["timeout"], " | ".join(m.get("stderr_head") or [])[:600]])
            time.sleep(20 * attempt)
            continue
        (out / "diff.patch").write_text(diff)
        files = sorted(set(re.findall(r"^\+\+\+ b/(\S+)", diff, re.M)))
        auto = classify.classify(case, base / "work")
        row = {"trial_id": t, "case": case, "tool": tool, "model": m["model_arg"], "model_reported": m.get("model_reported"), "cli_version": m["cli_version"],
               "variant": variant, "rep": rep, "date": m["date"], "duration_s": m["duration_s"], "tokens_in": m.get("tokens_in"), "tokens_out": m.get("tokens_out"),
               "cost_usd": m.get("cost_usd"), "harness_config_hash": m["harness_config_hash"], "prompt_sha256": m["prompt_sha256"], "cli_flags": m["cli_flags"],
               "exit_code": m["exit_code"], "files_changed": ";".join(files), "diff_lines": len(diff.splitlines()), "diff_sha256": hashlib.sha256(diff.encode()).hexdigest()[:16],
               "net_tool_calls": net_calls(tool, out), "attempts": attempt, "status": ("harness-halt" if "no output produced" in stderr_txt else "infra-failed") if infra else "ok"}
        arow = {"trial_id": t, "case": case, "auto_outcome": auto[0], "auto_rules": ";".join(auto[1]), "auto_evidence": auto[2], "auto_tags": ";".join(auto[3])}
        pre = "pilot_" if pilot else ""
        with LOCK:
            for name, cols, r in ((pre + "trials_meta.csv", META_COLS, row), (pre + "auto_labels.csv", AUTO_COLS, arow)):
                p = E4 / "results" / name
                new = not p.exists()
                with open(p, "a", newline="") as f:
                    w = csv.DictWriter(f, cols, lineterminator="\n")
                    if new: w.writeheader()
                    w.writerow(r)
        (out / "meta.json").write_text(json.dumps({**m, **row, "auto": auto}, indent=2, default=str))
        return t, row["status"] + ":" + auto[0]
    return t, "unreachable"

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--reps", type=int, default=5); ap.add_argument("--rep-start", type=int, default=1)
    ap.add_argument("--tools", default="claude,codex,agy"); ap.add_argument("--cases", default=",".join(CASES)); ap.add_argument("--variants", default="A,B")
    ap.add_argument("--parallel", type=int, default=3); ap.add_argument("--timeout", type=int, default=900); ap.add_argument("--pilot", action="store_true")
    a = ap.parse_args()
    if not a.pilot: check_rubric()
    gl.ensure_stub()
    runs_dir = E4 / ("runs-pilot" if a.pilot else "runs"); (E4 / "results").mkdir(exist_ok=True)
    jobs = [(c, t, v, r) for r in range(a.rep_start, a.rep_start + a.reps) for c in a.cases.split(",") for t in a.tools.split(",") for v in a.variants.split(",")]
    def job(j):
        c, t, v, r = j
        return run_one(c, t, v, r, a.pilot, runs_dir, a.timeout)
    with cf.ThreadPoolExecutor(a.parallel) as ex:  # at most --parallel generations in flight (alpha: max 2, RAM)
        for t, st in ex.map(job, jobs):
            print(t, st, flush=True)
