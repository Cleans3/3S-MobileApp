#!/usr/bin/env python3
"""Run ONE generator CLI turn in an isolated dir with an empty harness config (contract R2).

Used by E4 (pressure) and E5 (optimizer). Heavy/secret state lives outside the repo:
  STUB_HOME = <study-work>/stubs/home   (only auth files copied in; no CLAUDE.md/AGENTS.md/settings/MCP/skills)
  runs      = <study-work>/trials/<id>/ (work copy, raw logs)

Library use:  from gen_launch import run_generator, ensure_stub, config_hash, TOOLS
CLI use:      gen_launch.py --tool codex --workdir DIR --prompt-file P [--model M] [--timeout S] [--out DIR] [--real-home]
--real-home = positive control for the canary pilot (loads the user's real configs). Never use it for trials.
"""
import argparse, fcntl, gzip, hashlib, json, os, pathlib, shutil, subprocess, sys, threading, time

WORK = pathlib.Path("/mnt/01DC198BE9192500/github/Schooldev/3S-study-work")
STUB_HOME = WORK / "stubs" / "home"
CLAUDE_TMP = pathlib.Path("/tmp/b24-claude-tmp")  # /mnt is fuseblk: claude refuses a tmp dir it cannot chown-check
REAL_HOME = pathlib.Path.home()

# tool key -> default model (pinned, recorded in runs); haiku is the only Anthropic generator allowed (spawn-tier rule)
TOOLS = {
    "claude":   {"model": "haiku",         "bin": "claude"},
    "codex":    {"model": "gpt-5.6-luna",  "bin": "codex"},
    "agy":      {"model": "gemini-3.8-flash-medium", "bin": "agy"},  # Gemini via Antigravity CLI; gemini-cli is dead here (REPORT)
    "opencode": {"model": "opencode/big-pickle", "bin": "opencode"},
}

# auth files: copied fresh from the real location into every per-run home, and written back (compare-and-swap)
# if the tool refreshed/rotated the token, so a stub run can never log the user's real session out.
AUTH = {  # path inside a home -> real location
    ".claude/.credentials.json": lambda: pathlib.Path(os.environ.get("CLAUDE_CONFIG_DIR", REAL_HOME / ".claude")) / ".credentials.json",
    ".codex/auth.json": lambda: REAL_HOME / ".codex" / "auth.json",
    ".local/share/opencode/auth.json": lambda: REAL_HOME / ".local/share/opencode/auth.json",
    ".gemini/oauth_creds.json": lambda: REAL_HOME / ".gemini" / "oauth_creds.json",       # agy
    ".gemini/google_accounts.json": lambda: REAL_HOME / ".gemini" / "google_accounts.json",
}

def ensure_stub(force=False):
    """Pristine non-secret stub (the recorded 'harness config'). Auth is added per run, not stored here."""
    if STUB_HOME.exists() and not force:
        return STUB_HOME
    shutil.rmtree(STUB_HOME, ignore_errors=True)
    (STUB_HOME / ".claude").mkdir(parents=True)
    (STUB_HOME / ".claude" / ".claude.json").write_text(json.dumps({"hasCompletedOnboarding": True, "numStartups": 5}))
    return STUB_HOME

def config_hash():
    """sha256 over the pristine stub: every file path + content, plus the names of the auth files added per run."""
    h = hashlib.sha256()
    for p in sorted(STUB_HOME.rglob("*")):
        if p.is_file():
            h.update(f"{p.relative_to(STUB_HOME).as_posix()}\n".encode()); h.update(p.read_bytes())
    h.update("\n".join(sorted(AUTH)).encode())
    return h.hexdigest()

_mine = 0
_mine_lock = threading.Lock()
HOLD_POLL = 5

def _hold_active():
    """WORK/bench.hold is written by run_bench (f2-3) for its measurement window: first token = its pid. A file from a dead pid, or an unreadable one, is ignored."""
    try: pid = int((WORK / "bench.hold").read_text().split()[0])
    except (OSError, ValueError, IndexError): return False
    return pathlib.Path(f"/proc/{pid}").exists()

def _shared_held_by_this_process():
    """READ flocks on bench.lock held by this process (any fd/thread), from /proc/locks."""
    try:
        ino, me, n = str(os.stat(WORK / "bench.lock").st_ino), str(os.getpid()), 0
        for ln in open("/proc/locks"):
            f = ln.split()
            if "FLOCK" in f and "READ" in f:
                i = f.index("READ")
                if f[i + 1] == me and f[i + 2].split(":")[-1] == ino: n += 1
        return n
    except (OSError, ValueError):
        return 0

class bench_shared:
    """Shared bench lock for the duration of a generation (R1: benchmarks take it exclusively).
    Acquire goes through the turnstile so a waiting benchmark (LOCK_EX) is not starved by overlapping LOCK_SH holders (alpha, 2026-11-21).
    Re-entrant across callers: if the calling process already holds a shared bench.lock that this class did not take (e1-corpus generate.py
    takes LOCK_SH itself, then calls run_generator), do nothing; without this a nested turnstile wait deadlocks against a waiting benchmark.
    _mine_lock only guards the counter and is NEVER held across a blocking flock: holding it while waiting for the turnstile stopped a
    finishing worker from releasing its READ lock (3-way wait with f2-3's run_bench, reported by beta-f2-3 as Q#dl-1)."""
    def __enter__(self):
        global _mine
        self.lk = None
        with _mine_lock:
            if _shared_held_by_this_process() > _mine: return  # nested in a caller that already holds the lock: work in flight finishes, no hold wait
        while _hold_active(): time.sleep(HOLD_POLL)  # E2 measurement window: start nothing new; outside _mine_lock
        with _mine_lock:
            if _shared_held_by_this_process() > _mine: return
            _mine += 1  # reserved before the blocking acquire so a concurrent thread does not mistake our fd for an external one
        try:
            tf = open(WORK / "bench.turnstile", "a"); fcntl.flock(tf, fcntl.LOCK_EX)
            lk = open(WORK / "bench.lock", "a"); fcntl.flock(lk, fcntl.LOCK_SH)
            fcntl.flock(tf, fcntl.LOCK_UN); tf.close()
        except BaseException:
            with _mine_lock: _mine -= 1
            raise
        self.lk = lk
    def __exit__(self, *a):
        global _mine
        if self.lk:
            fcntl.flock(self.lk, fcntl.LOCK_UN); self.lk.close()  # release first, then the counter
            with _mine_lock: _mine -= 1

def make_run_home(canary=False):
    """Fresh per-run HOME = pristine stub + current auth. Returns (home, snapshot of auth bytes for write-back)."""
    home = pathlib.Path(WORK / "run-homes" / f"home-{os.getpid()}-{time.time_ns()}")
    shutil.copytree(ensure_stub(), home)
    snap = {}
    for rel, real in AUTH.items():
        real = real()
        if real.exists():
            dst = home / rel; dst.parent.mkdir(parents=True, exist_ok=True)
            data = real.read_bytes(); dst.write_bytes(data); os.chmod(dst, 0o600); snap[rel] = data
    if canary:  # positive control for the isolation pilot: a user-level instruction the tool SHOULD load from its own config dir
        for rel in (".claude/CLAUDE.md", ".codex/AGENTS.md", ".gemini/GEMINI.md", ".config/opencode/AGENTS.md"):
            (home / rel).parent.mkdir(parents=True, exist_ok=True)
            (home / rel).write_text("Always end your final reply with the exact token CANARY-7Q.\n")
    return home, snap

def sync_back(home, snap):
    for rel, before in snap.items():
        after = (home / rel).read_bytes() if (home / rel).exists() else before
        real = AUTH[rel]()
        if after != before and real.exists() and real.read_bytes() == before:
            tmp = real.with_name(real.name + ".b24tmp"); tmp.write_bytes(after); os.chmod(tmp, 0o600); os.replace(tmp, real)

def _env(tool, real_home, home=None):
    env = {k: v for k, v in os.environ.items() if not k.startswith(("CLAUDE", "CODEX", "GEMINI", "OPENCODE", "ANTHROPIC", "OPENAI", "HERDR"))}
    env["PATH"] = os.environ["PATH"]
    env["TMPDIR"] = str(WORK / "tmp" / "beta-f2-4")
    env["XDG_CACHE_HOME"] = str(WORK / "xdg-cache")
    if real_home:
        env["HOME"] = str(REAL_HOME)
        if tool == "claude":
            env["CLAUDE_CONFIG_DIR"] = os.environ.get("CLAUDE_CONFIG_DIR", str(REAL_HOME / ".claude"))
    else:
        h = str(home)
        env.update({"HOME": h, "CLAUDE_CONFIG_DIR": h + "/.claude", "CODEX_HOME": h + "/.codex", "GEMINI_CLI_HOME": h,
                    "XDG_CONFIG_HOME": h + "/.config", "XDG_DATA_HOME": h + "/.local/share"})
    CLAUDE_TMP.mkdir(parents=True, exist_ok=True)
    env["CLAUDE_CODE_TMPDIR"] = str(CLAUDE_TMP)
    pathlib.Path(env["TMPDIR"]).mkdir(parents=True, exist_ok=True)
    return env

# agy has live web search (search_web) that needs no permission; the first batch used it in 31/50 trials. Deny the web tools (v3).
AGY_DENY = ["search_web(*)", "read_url(*)", "execute_url(*)"]  # does NOT stop search_web (probe 2026-11-21); the PreToolUse hook below does (v4)
AGY_WEB_HOOK = {"hooks": {"PreToolUse": [{"matcher": "search_web|read_url_content|read_url|execute_url|browser.*", "hooks": [{"type": "command", "command": "python3 " + str(pathlib.Path(__file__).resolve().parent / "agy_deny_web_hook.py")}]}]}}  # goes to ~/.gemini/antigravity-cli/hooks.json of the run HOME; --settings rejects a "hooks" key

# v4: codex reaches the web through the built-in web_search item AND through the codex_apps MCP tool search_service.web_run (feature "apps"); close both plus the browser/computer-use features
CODEX_NO_NET = [x for f in ("apps", "browser_use", "browser_use_external", "computer_use", "plugins", "in_app_browser") for x in ("--disable", f)]

def _cmd(tool, workdir, prompt, model):
    if tool == "claude":  # file tools only (no shell): the permission system stays on; writes are additionally contained by jail_exec.py
        return ["claude", "-p", prompt, "--model", model, "--tools", "Read,Edit,Write,Glob,Grep",
                "--permission-mode", "acceptEdits", "--strict-mcp-config", "--disable-slash-commands",
                "--no-session-persistence", "--output-format", "stream-json", "--verbose", "--max-budget-usd", "2"]
    if tool == "codex":
        # -s danger-full-access: codex's bubblewrap sandbox cannot start on this host (AppArmor userns restriction); jail_exec.py contains the writes
        return ["codex", "exec", "--json", "-s", "danger-full-access", "--skip-git-repo-check", "--ephemeral",
                "-C", str(workdir), "-m", model,
                "-c", 'model_reasoning_effort="medium"', "-c", 'web_search="disabled"', *CODEX_NO_NET, prompt]  # v4: built-in web_search exists in exec mode (probe 2026-11-21); the 50 primary E4 codex trials ran without this flag and used it 0 times
    if tool == "agy":  # print mode: file edits ok; shell only for the read-only-ish commands below. Headless mode ends the whole run at the first
        # command that is not pre-approved ('jetski: no output produced - a tool required the "command" permission', pilot 2026-11-21), so the list is wide;
        # a run that still ends that way with an empty diff is retried and, if it persists, reported as harness-limited, not as no-fix.
        allow = ["ls", "cat", "find", "grep", "head", "tail", "sed", "wc", "tree", "git", "pwd", "echo", "stat", "file", "awk", "sort", "uniq", "diff", "cut", "xargs", "basename", "dirname", "realpath", "which", "openssl", "keytool", "jq", "du", "readlink", "xxd", "strings", "unzip", "tr", "ls"]
        return ["agy", "--print", prompt, "--model", model, "--output-format", "stream-json", "--mode", "accept-edits",
                "--settings", json.dumps({"permissions": {"allow": [f"command({c})" for c in allow], "deny": AGY_DENY}})]
    if tool == "opencode":
        return ["opencode", "run", "--format", "json", "--auto", "--dir", str(workdir), "-m", model, prompt]
    raise ValueError(tool)

def _parse(tool, lines):
    """Pull model / tokens / cost out of the tool's event stream. Tolerant: unknown shape -> None."""
    info = {"model_reported": None, "tokens_in": None, "tokens_out": None, "cost_usd": None, "final_text": None}
    for ln in lines:
        try:
            e = json.loads(ln)
        except Exception:
            continue
        if tool == "claude":
            if e.get("type") == "system" and e.get("subtype") == "init":
                info["model_reported"] = e.get("model")
            if e.get("type") == "result":
                u = e.get("usage") or {}
                info["tokens_in"] = (u.get("input_tokens") or 0) + (u.get("cache_read_input_tokens") or 0) + (u.get("cache_creation_input_tokens") or 0)
                info["tokens_out"] = u.get("output_tokens")
                info["cost_usd"] = e.get("total_cost_usd")
                info["final_text"] = e.get("result")
        elif tool == "codex":
            t = e.get("type")
            if t == "turn.completed":
                u = e.get("usage") or {}
                info["tokens_in"] = (info["tokens_in"] or 0) + (u.get("input_tokens") or 0)
                info["tokens_out"] = (info["tokens_out"] or 0) + (u.get("output_tokens") or 0)
            it = e.get("item") or {}
            if t == "item.completed" and it.get("type") == "agent_message":
                info["final_text"] = it.get("text")
            for k in ("model",):
                if e.get(k): info["model_reported"] = e[k]
        elif tool == "agy":
            if e.get("event") == "result":
                r = e.get("result") or {}; u = r.get("usage") or {}
                info["tokens_in"], info["tokens_out"], info["final_text"] = u.get("input_tokens"), u.get("output_tokens"), r.get("response")
        elif tool == "opencode":
            p = e.get("part") or {}
            if e.get("type") == "text":
                info["final_text"] = p.get("text")
            if e.get("type") == "step_finish":
                t = p.get("tokens") or {}
                info["tokens_in"] = (info["tokens_in"] or 0) + (t.get("input") or 0)
                info["tokens_out"] = (info["tokens_out"] or 0) + (t.get("output") or 0)
                info["cost_usd"] = (info["cost_usd"] or 0) + (p.get("cost") or 0)
    return info

def version(tool):
    r = subprocess.run([TOOLS[tool]["bin"], "--version"], capture_output=True, text=True, env={**os.environ, "CLAUDE_CODE_TMPDIR": str(CLAUDE_TMP)})
    return (r.stdout or r.stderr).strip().splitlines()[0] if (r.stdout or r.stderr).strip() else "?"

def _gz(path, data):
    with gzip.open(path, "wb") as f: f.write(data)

def _retry(fn, tries=6, wait=20):
    """The repo volume (fuseblk) returned a transient ENOSPC at 2026-11-21 12:30 while df showed 330 GB free; the generation is expensive, so retry the write."""
    for i in range(tries):
        try: return fn()
        except OSError:
            if i == tries - 1: raise
            time.sleep(wait)

def run_generator(tool, workdir, prompt, out_dir=None, model=None, timeout=900, real_home=False, canary=False, readonly=False):
    """One non-interactive turn in a fresh per-run HOME. Returns meta dict; raw stdout/stderr gzipped into out_dir."""
    model = TOOLS[tool]["model"] if model is None else model
    workdir = pathlib.Path(workdir)
    home, snap = (None, {}) if real_home else make_run_home(canary)
    if tool == "agy" and home:
        hp = home / ".gemini" / "antigravity-cli" / "hooks.json"; hp.parent.mkdir(parents=True, exist_ok=True); hp.write_text(json.dumps(AGY_WEB_HOOK, indent=1))
    env = _env(tool, real_home, home)
    cmd = _cmd(tool, workdir, prompt, model)
    if not real_home:  # Landlock write-jail: the run may write only to its workdir, its own HOME and the scratch dirs
        rw = ([] if readonly else [str(workdir)]) + [str(home), env["TMPDIR"], env["XDG_CACHE_HOME"], str(CLAUDE_TMP), "/dev"]  # readonly = advice-only runs (E5)
        cmd = [sys.executable, str(pathlib.Path(__file__).with_name("jail_exec.py")), *[x for d in rw for x in ("--rw", d)], "--", *cmd]
    with bench_shared():
        t0 = time.time()  # after the lock wait (earlier trials measured from before it: duration_s then includes the wait)
        try:
            p = subprocess.run(cmd, cwd=workdir, env=env, capture_output=True, text=True, timeout=timeout, stdin=subprocess.DEVNULL)
            rc, so, se, to = p.returncode, p.stdout, p.stderr, False
        except subprocess.TimeoutExpired as ex:
            rc, so, se, to = -9, (ex.stdout or b"").decode() if isinstance(ex.stdout, bytes) else (ex.stdout or ""), (ex.stderr or b"").decode() if isinstance(ex.stderr, bytes) else (ex.stderr or ""), True
    if home:
        sync_back(home, snap)
    dur = round(time.time() - t0, 1)
    meta = {"tool": tool, "model_arg": model, "cli_version": version(tool), "exit_code": rc, "timeout": to,
            "duration_s": dur, "date": time.strftime("%Y-%m-%d"), "real_home": real_home,
            "harness_config_hash": None if real_home else config_hash(), "canary_planted": canary,
            "prompt_sha256": hashlib.sha256(prompt.encode()).hexdigest(),
            "cli_flags": " ".join(c if c != prompt else "<PROMPT>" for c in _cmd(tool, workdir, prompt, model)[1:])}
    meta.update(_parse(tool, so.splitlines()))
    meta["stderr_head"] = se.strip().splitlines()[:5]
    if out_dir:
        out = pathlib.Path(out_dir); out.mkdir(parents=True, exist_ok=True)
        _retry(lambda: _gz(out / "transcript.jsonl.gz", so.encode()))
        _retry(lambda: _gz(out / "stderr.txt.gz", se.encode()))
        if tool == "agy":  # richer per-step log (tool args) than the stdout stream
            for p in (home or REAL_HOME).glob(".gemini/antigravity-cli/brain/*/.system_generated/logs/transcript_full.jsonl"):
                _retry(lambda: _gz(out / "transcript_full.jsonl.gz", p.read_bytes()))
        _retry(lambda: (out / "meta.json").write_text(json.dumps(meta, indent=2)))
    if home:
        shutil.rmtree(home, ignore_errors=True)
    return meta

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--tool", required=True, choices=list(TOOLS))
    ap.add_argument("--workdir", required=True)
    ap.add_argument("--prompt-file", required=True)
    ap.add_argument("--model"); ap.add_argument("--timeout", type=int, default=900)
    ap.add_argument("--out"); ap.add_argument("--real-home", action="store_true"); ap.add_argument("--canary", action="store_true")
    a = ap.parse_args()
    m = run_generator(a.tool, a.workdir, pathlib.Path(a.prompt_file).read_text(), a.out, a.model, a.timeout, a.real_home, a.canary)
    print(json.dumps(m, indent=2))
