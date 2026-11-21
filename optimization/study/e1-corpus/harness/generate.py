#!/usr/bin/env python3
"""E1 generator harness: one run = one (bundle, brief, style, rep) in a fresh dir, via the shared isolated launcher.

  generate.py one  --bundle claude-haiku --brief B1 --style naive --rep 1 [--pilot]
  generate.py plan                       # enumerate + shuffle all non-pilot cells (seed 20261121) -> results/plan.json
  generate.py campaign [--workers 2] [--limit N] [--bundles a,b]   # run the plan, resumable, rep-major

Isolation, tool restrictions, Landlock write-jail and auth write-back live in e4-pressure/harness/gen_launch.py (owner beta-f2-4,
imported read-only). Every generation holds the SHARED bench.lock (f2-3 takes it exclusive while benchmarking).
Per-run truth = runs/<run_id>/meta.json; results/runs.csv is generated from it by collect.py.
"""
import argparse, concurrent.futures as cf, fcntl, gzip, hashlib, json, os, pathlib, random, re, shutil, sys, time

HERE = pathlib.Path(__file__).resolve().parent
E1 = HERE.parent
STUDY = E1.parent
WORK = pathlib.Path("/mnt/01DC198BE9192500/github/Schooldev/3S-study-work")
sys.path.insert(0, str(STUDY / "e4-pressure" / "harness"))
import gen_launch  # noqa: E402  (read-only reuse)

# E1-local: agy headless ends the whole run at the first non-allow-listed command (f2-4 pilot, repeated in the E1 pilot: B1 naive died on
# 'java -version' with 0 files). A user would click "allow" on workspace housekeeping, so E1 adds file-system housekeeping (writes stay in the
# Landlock jail) and read-only toolchain probes. Network, install and build tools (curl, wget, pip, npm, gradle, adb, sdkmanager) stay denied.
AGY_EXTRA_ALLOW = ["mkdir", "touch", "chmod", "cp", "mv", "rm", "java", "javac", "kotlinc", "uname", "date", "whoami"]
_orig_cmd = gen_launch._cmd

def _cmd_e1(tool, workdir, prompt, model):
    c = _orig_cmd(tool, workdir, prompt, model)
    if tool == "agy":
        i = c.index("--settings"); s = json.loads(c[i + 1])
        allow = s["permissions"]["allow"]
        s["permissions"]["allow"] = allow + [f"command({x})" for x in AGY_EXTRA_ALLOW if f"command({x})" not in allow]
        c[i + 1] = json.dumps(s)
    return c

gen_launch._cmd = _cmd_e1

SEED = 20261121
BRIEFS = ["B1", "B2", "B3"]
STYLES = ["naive", "detailed-functional", "guardrailed"]
# bundle key -> (launcher tool, model arg). "gemini" bundle = agy (contract 6c); opencode is optional.
BUNDLES = {
    "claude-haiku": ("claude", "haiku"),
    "codex-luna": ("codex", "gpt-5.6-luna"),
    "agy-g38flash": ("agy", "gemini-3.8-flash-medium"),
    "opencode-bigpickle": ("opencode", "opencode/big-pickle"),
}
CTX_FILE = {"claude": "CLAUDE.md", "codex": "AGENTS.md", "agy": "GEMINI.md", "opencode": "AGENTS.md"}  # protocol 8; agy checked by canary_ctx
GUARDRAIL = STUDY / "guardrail" / "ANDROID-GUARDRAIL.md"
BENCH_LOCK = WORK / "bench.lock"
TIMEOUT_S = 1500
INFRA_RE = re.compile(r"rate.?limit|429|quota|usage limit|exhausted|overloaded|capacity|credit|too many requests|unauthori[sz]ed|401|ECONNRESET|ETIMEDOUT|ENOTFOUND", re.I)
NET_RE = re.compile(r"\b(curl|wget|pip3? install|npm (?:i|install)|apt(?:-get)? (?:install|update)|git clone|git fetch|git pull|ssh|scp|nc|sdkmanager|adb|mvn|docker)\b|(?:^|[;&|(]\s*)(?:(?:sh|bash)\s+)?(?:\./)?gradlew?(?![\w.\-])|\./gradlew")
TEXT_RE = re.compile(r"\b(curl|wget|pip3? install|npm (?:i|install)|apt(?:-get)? (?:install|update)|git clone|git fetch|git pull|sdkmanager|\./gradlew|adb (?:install|shell)|docker (?:run|pull))\b")
CMD_KEYS = {"command", "cmd", "commandLine", "command_line", "CommandLine", "shell_command"}

def _cmds(o, out):
    """Collect shell-command strings from any transcript JSON object (codex command_execution, claude Bash tool_use, agy run_command...)."""
    if isinstance(o, dict):
        for k, v in o.items():
            if k in CMD_KEYS and isinstance(v, (str, list)):
                out.append(v if isinstance(v, str) else " ".join(map(str, v)))
            else:
                _cmds(v, out)
    elif isinstance(o, list):
        for v in o: _cmds(v, out)

def net_audit(trans):
    """R2 scope audit: did the generator run network/build/install commands? Scans executed command strings only
    (file contents the generator wrote, e.g. build.gradle, would false-positive on a whole-transcript scan).
    If a transcript yields no command strings, fall back to a stricter whole-text scan and tag it 'text:'."""
    cmds = []
    for line in trans.splitlines():
        try: _cmds(json.loads(line), cmds)
        except ValueError: pass
    if cmds:
        hits = sorted(set(m.group(0).lstrip(";&|( ") for c in cmds for m in NET_RE.finditer(c)))
        return ",".join(hits) or "none", len(cmds)
    hits = sorted(set(m.group(1) for m in TEXT_RE.finditer(trans)))
    return ("text:" + ",".join(hits) if hits else "none"), 0

def sha(b): return hashlib.sha256(b).hexdigest()

def guardrail_sha():
    want = (STUDY / "guardrail" / "ANDROID-GUARDRAIL.sha256").read_text().split()[0]
    got = sha(GUARDRAIL.read_bytes())
    assert got == want, f"guardrail changed: {got} != frozen {want}"
    return got

def prompt_for(brief, style):
    f = E1 / "prompts" / f"{brief}-{'naive' if style == 'naive' else 'detailed-functional'}.txt"
    return f.read_text()

def run_id(bundle, brief, style, rep, pilot=False):
    return f"{'pilot-' if pilot else ''}{bundle}-{brief}-{style}-r{rep}"

def kt_loc(root):
    n = 0
    for p in root.rglob("*.kt"):
        for ln in p.read_text(errors="replace").splitlines():
            s = ln.strip()
            if s and not s.startswith(("//", "/*", "*")): n += 1
    return n

def sources(wd, ctx):
    skip = {".gradle", "build", ".git", ".idea", ".kotlin"}
    for p in sorted(wd.rglob("*")):
        rel = p.relative_to(wd)
        if p.is_file() and not (set(rel.parts) & skip) and rel.as_posix() != ctx and p.name != "local.properties":
            yield rel

def denied_actions(trans):
    """agy headless: first command outside the allow-list auto-denies and ends the whole run (f2-4 pilot, repeated in E1 pilot).
    The result event lists denied_actions; a non-empty list marks a possibly truncated run."""
    n = 0
    for line in trans.splitlines():
        try: o = json.loads(line)
        except ValueError: continue
        if isinstance(o, dict) and isinstance(o.get("result"), dict):
            n = max(n, len(o["result"].get("denied_actions") or []))
    return n

WEB_NAME = re.compile(r"^(search_web|read_url_content|read_url|execute_url|browser_\w+|\w*browser\w*|WebSearch|WebFetch|web_search|web_fetch)$", re.I)

def web_audit(trans):
    """R2 no-retrieval audit over tool invocations (not command strings): agy step_update tool_name, codex item.type, claude tool_use name.
    Counts attempts, including ones a PreToolUse hook then denied. Returns (n_calls, sorted tool names)."""
    seen, names = set(), []
    for line in trans.splitlines():
        try: o = json.loads(line)
        except ValueError: continue
        if not isinstance(o, dict): continue
        cand = []
        su = o.get("step_update")
        if isinstance(su, dict) and su.get("step_type") == "tool":
            cand.append((("agy", su.get("conversation_id"), su.get("step_index")), su.get("tool_name") or ""))
        it = o.get("item")
        if isinstance(it, dict) and o.get("type") == "item.completed":
            cand.append((("codex", it.get("id")), it.get("type") or ""))
        if o.get("type") == "assistant":
            for b in (o.get("message") or {}).get("content") or []:
                if isinstance(b, dict) and b.get("type") == "tool_use": cand.append((("claude", b.get("id")), b.get("name") or ""))
        for key, name in cand:
            if WEB_NAME.match(name) and key not in seen: seen.add(key); names.append(name)
    return len(names), sorted(set(names))

def web_denied(tool, cli_flags=None):
    """y when the launcher module in THIS process carries f2-4's agy web-deny hook (it writes hooks.json into the per-run HOME), n for agy without it, na for bundles with no web tool."""
    return ("y" if hasattr(gen_launch, "AGY_WEB_HOOK") else "n") if tool == "agy" else "na"

def read_gz(p):
    try:
        with gzip.open(p, "rt", errors="replace") as f: return f.read()
    except FileNotFoundError: return ""

def run_one(bundle, brief, style, rep, pilot=False, timeout=TIMEOUT_S):
    tool, model = BUNDLES[bundle]
    rid = run_id(bundle, brief, style, rep, pilot)
    out = E1 / "runs" / rid
    if (out / "meta.json").exists():
        return json.loads((out / "meta.json").read_text())
    if shutil.disk_usage("/").free < 1 << 30:
        raise SystemExit("root disk < 1 GB free: abort (contract R1)")
    prompt = prompt_for(brief, style)
    ctx = CTX_FILE[tool] if style == "guardrailed" else None
    attempts = []
    for attempt in (1, 2):
        wd, raw = WORK / "corpus" / rid, WORK / "corpus-raw" / (rid if attempt == 1 else rid + ".a2")
        shutil.rmtree(wd, ignore_errors=True); shutil.rmtree(raw, ignore_errors=True)
        wd.mkdir(parents=True)
        gsha = None
        if ctx:
            gsha = guardrail_sha(); shutil.copyfile(GUARDRAIL, wd / ctx)
        # the shared bench lock (through f2-3's turnstile) is taken inside gen_launch.run_generator: one lock site only (alpha, 2026-11-21 deadlock)
        meta = gen_launch.run_generator(tool, wd, prompt, out_dir=raw, model=model, timeout=timeout)
        files = list(sources(wd, ctx))
        kts = [f for f in files if f.suffix == ".kt"]
        gradle = [f for f in files if f.name.startswith("build.gradle")]
        stderr_txt = read_gz(raw / "stderr.txt.gz")
        t_all = read_gz(raw / "transcript.jsonl.gz")
        denied = denied_actions(t_all)
        rate = bool(INFRA_RE.search(stderr_txt + " ".join(meta.get("stderr_head") or []) + t_all[-3000:]))
        infra = (not files) and (rate or denied > 0)
        attempts.append({"attempt": attempt, "files": len(files), "exit_code": meta["exit_code"], "infra_failure": infra,
                         "denied_actions": denied, "harness_denied_empty": (not files) and denied > 0})
        if infra and attempt == 1:                       # protocol 4.1: regenerate once, only for infra failure before any file was written
            if not denied: time.sleep(300)               # backoff for rate limits; a denied-command stop is a harness limit, retry at once
            continue
        break
    trans = read_gz(raw / "transcript.jsonl.gz")
    net_hits, n_cmds = net_audit(trans)
    meta.update({
        "run_id": rid, "bundle": bundle, "brief": brief, "prompt_style": style, "rep": rep, "pilot": pilot,
        "files": len(files), "kotlin_loc": kt_loc(wd), "valid": bool(kts and gradle),
        "context_file": ctx, "guardrail_sha256": gsha, "attempts": attempts,
        "net_audit": net_hits, "n_commands": n_cmds, "denied_actions": denied,
        "web_tool_calls": web_audit(trans)[0], "web_tools": ",".join(web_audit(trans)[1]), "web_denied": web_denied(tool, meta.get("cli_flags")),
        "generated_at": time.strftime("%Y-%m-%dT%H:%M:%S%z"),
        "invalid_reason": "" if (kts and gradle) else ("harness-limited: agy headless denied a command before any file was written" if denied and not files else "no .kt or no gradle build file"),
        "transcript_path": f"optimization/study/e1-corpus/runs/{rid}/transcript.jsonl.gz",
        "temperature_or_seed": "not exposed by CLI",
    })
    (out / "src").mkdir(parents=True, exist_ok=True)
    for rel in files:
        dst = out / "src" / rel; dst.parent.mkdir(parents=True, exist_ok=True); shutil.copyfile(wd / rel, dst)
    for name in ("transcript.jsonl.gz", "stderr.txt.gz", "transcript_full.jsonl.gz"):
        if (raw / name).exists(): shutil.copyfile(raw / name, out / name)
    (out / "meta.json").write_text(json.dumps(meta, indent=2))
    return meta

def make_plan(bundles, reps):
    rng = random.Random(SEED)
    plan = []
    for rep in range(1, reps + 1):                     # rep-major: any prefix of the plan is a balanced design
        block = [(b, br, st, rep) for b in bundles for br in BRIEFS for st in STYLES]
        rng.shuffle(block); plan += block
    return plan

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("cmd", choices=["one", "plan", "campaign"])
    ap.add_argument("--bundle"); ap.add_argument("--brief"); ap.add_argument("--style"); ap.add_argument("--rep", type=int, default=1)
    ap.add_argument("--pilot", action="store_true")
    ap.add_argument("--bundles", default="claude-haiku,codex-luna,agy-g38flash")
    ap.add_argument("--reps", type=int, default=4); ap.add_argument("--workers", type=int, default=2); ap.add_argument("--limit", type=int)
    a = ap.parse_args()
    if a.cmd == "one":
        m = run_one(a.bundle, a.brief, a.style, a.rep, a.pilot)
        print(json.dumps({k: m.get(k) for k in ("run_id", "valid", "files", "kotlin_loc", "duration_s", "exit_code", "tokens_in", "tokens_out", "cost_usd", "net_audit", "attempts")}, indent=1))
        return
    bundles = a.bundles.split(",")
    plan = make_plan(bundles, a.reps)
    pf = E1 / "results" / "plan.json"
    if a.cmd == "plan":
        pf.write_text(json.dumps({"seed": SEED, "bundles": bundles, "reps": a.reps, "cells": plan}, indent=0)); print(len(plan), "cells ->", pf); return
    todo = [c for c in plan if not (E1 / "runs" / run_id(*c) / "meta.json").exists()][: a.limit]
    print(len(todo), "cells to run,", a.workers, "workers", flush=True)
    with cf.ThreadPoolExecutor(a.workers) as ex:
        futs = {ex.submit(run_one, *c): c for c in todo}
        for f in cf.as_completed(futs):
            c = futs[f]
            try: m = f.result(); print("done", m["run_id"], "valid" if m["valid"] else "INVALID", m["duration_s"], "s", flush=True)
            except Exception as e:
                print("ERR", c, repr(e), flush=True)
                if isinstance(e, OSError) and e.errno == 28:        # ENOSPC (the NTFS volume runs out of file records): stop, do not burn the whole plan
                    for g in futs: g.cancel()
                    print("ABORT: ENOSPC, rerun the same command after freeing files", flush=True); break

if __name__ == "__main__":
    main()
