#!/usr/bin/env python3
"""Web/network tool-use audit of generator transcripts (stdlib). One row per trial dir.
  audit_web.py RUNS_DIR [RUNS_DIR ...] [--out web_audit.csv]
Counts, per trial, calls that can reach the outside world:
  builtin_web   claude WebSearch/WebFetch; codex web_search item or mcp_tool_call to a search/fetch/web/browse tool; agy search_web/read_url*/browse*
  web_denied    of those, calls the harness refused (agy PreToolUse hook error text)
  shell_net     shell commands whose command word is a network client (curl wget nc ssh scp ftp telnet, git clone/fetch/pull/push, pip/npm/yarn/gradle installs)
flag = builtin_web>0 or shell_net>0 ; reached = flag and not all denied. Needs transcript.jsonl.gz (+ transcript_full.jsonl.gz for agy) and meta.json in each dir."""
import csv, glob, gzip, json, pathlib, re, shlex, sys
WEB = re.compile(r"(search|fetch|web|browse|url|http)", re.I)
NETCMD = {"curl", "wget", "nc", "ncat", "ssh", "scp", "sftp", "ftp", "telnet", "pip", "pip3", "npm", "npx", "yarn", "pnpm"}
GITNET = {"clone", "fetch", "pull", "push", "ls-remote"}

def load(p):
    out = []
    try:
        with gzip.open(p, "rt", errors="replace") as f:
            for l in f:
                l = l.strip()
                if l:
                    try: out.append(json.loads(l))
                    except ValueError: pass
    except OSError: pass
    return out

def shell_net(cmd):
    """count network-client invocations in a shell command string (descends into bash -lc "...")"""
    n = 0
    try: toks = shlex.split(cmd)
    except ValueError: toks = cmd.split()
    inner = [t for i, t in enumerate(toks) if i and toks[i - 1] in ("-c", "-lc", "-ic") ]
    if inner: return sum(shell_net(t) for t in inner)
    for seg in re.split(r"\|\||&&|[;|\n]", cmd):
        try: w = shlex.split(seg)
        except ValueError: w = seg.split()
        while w and re.match(r"^\w+=", w[0]): w = w[1:]
        if not w: continue
        c = pathlib.Path(w[0]).name
        if c in NETCMD or (c == "git" and len(w) > 1 and w[1] in GITNET) or (c in ("python", "python3") and any(re.search(r"urllib|requests|http\.client|socket", x) for x in w[1:])): n += 1
    return n

def audit(d):
    d = pathlib.Path(d); meta = json.load(open(d / "meta.json")); tool = meta.get("tool", "")
    web = den = net = 0
    if tool.startswith("claude"):
        for e in load(d / "transcript.jsonl.gz"):
            if e.get("type") == "assistant":
                for b in e.get("message", {}).get("content", []) or []:
                    if isinstance(b, dict) and b.get("type") == "tool_use":
                        if b["name"] in ("WebSearch", "WebFetch") or WEB.search(b["name"]): web += 1
                        if b["name"] == "Bash": net += shell_net(b.get("input", {}).get("command", ""))
    elif tool.startswith("codex"):
        for e in load(d / "transcript.jsonl.gz"):
            it = e.get("item", {}) if e.get("type") == "item.completed" else {}
            if it.get("type") == "web_search": web += 1
            elif it.get("type") == "mcp_tool_call" and WEB.search(str(it.get("tool", "")) + str(it.get("server", ""))) and it.get("tool") not in ("list_mcp_resources", "list_mcp_resource_templates"): web += 1
            elif it.get("type") == "command_execution": net += shell_net(it.get("command", ""))
    elif tool.startswith("agy"):
        for e in load(d / "transcript_full.jsonl.gz"):
            for tc in e.get("tool_calls") or []:
                nm = tc.get("name", "")
                if nm in ("search_web",) or re.match(r"(read_url|execute_url|browser)", nm): web += 1
                if nm == "run_command": net += shell_net(str((tc.get("args") or {}).get("CommandLine") or (tc.get("args") or {}).get("command") or tc.get("args")))
            if e.get("type") == "GENERIC" and "Web access is disabled" in str(e.get("content", "")): den += 1
    return {"trial": d.name, "tool": tool, "builtin_web": web, "web_denied": den, "shell_net": net,
            "flag": int(web > 0 or net > 0), "reached": int((web - den > 0) or net > 0)}

if __name__ == "__main__":
    a = sys.argv[1:]; out = "web_audit.csv"
    if "--out" in a: i = a.index("--out"); out = a[i + 1]; a = a[:i] + a[i + 2:]
    rows = [audit(d) for g in a for d in sorted(glob.glob(g.rstrip("/") + "/*")) if (pathlib.Path(d) / "meta.json").exists()]
    with open(out, "w", newline="") as fh:
        w = csv.DictWriter(fh, ["trial", "tool", "builtin_web", "web_denied", "shell_net", "flag", "reached"], lineterminator="\n"); w.writeheader(); w.writerows(rows)
    from collections import defaultdict
    s = defaultdict(lambda: [0, 0, 0, 0, 0])
    for r in rows:
        x = s[r["tool"]]; x[0] += 1; x[1] += r["flag"]; x[2] += r["reached"]; x[3] += r["builtin_web"]; x[4] += r["shell_net"]
    for t, x in sorted(s.items()): print(f"{t}: trials={x[0]} flagged={x[1]} reached={x[2]} web_calls={x[3]} shell_net_cmds={x[4]}")
