#!/usr/bin/env python3
"""Isolation audit: scan EVERY generation transcript for factory-leak markers. Read-only on inputs; writes only into isolation-audit/."""
import gzip, json, os, re, csv, sys, collections, pathlib
HERE = pathlib.Path(__file__).resolve().parent
STUDY = HERE.parent
WORK = pathlib.Path("/mnt/01DC198BE9192500/github/Schooldev/3S-study-work")
LEAK = {  # instruction-bearing markers: any hit outside the init-path block is a candidate leak
 "CAVEMAN": r"caveman", "PONYTAIL": r"ponytail", "context-mode": r"context[-_]mode", "ctx_tool": r"\bctx_[a-z]+",
 "jcodemunch": r"jcodemunch", "Cleanse": r"\bCleanse\b", "herdr": r"herdr", "dot-agents": r"\.agents\b",
 "rtk": r"\brtk\b", "laziest": r"laziest", "YAGNI": r"\bYAGNI\b", "mods": r"compact-pilot|model-gate|usage-meter",
 "tuvn": r"\btuvn", "homepath": r"/home/cleanse", "factory(weak)": r"\bfactory\b",
}
INFO = {"CANARY": r"CANARY-(7Q|CTX)", "ctxfile": r"\b(CLAUDE|AGENTS|GEMINI)\.md\b"}
LEAK_RE = {k: re.compile(v, re.I if k not in ("Cleanse",) else 0) for k, v in LEAK.items()}
INFO_RE = {k: re.compile(v) for k, v in INFO.items()}
INIT_STRIP = re.compile(r'"plugins":\s*\[.*?\],\s*"capabilities"', re.S)
MEM_STRIP = re.compile(r'"(memory_paths|auto)":\s*[^,}]*(\{[^}]*\})?')
ART = re.compile(r"\b(the|a|an)\b", re.I)
WORD = re.compile(r"[A-Za-z']+")

def rd(p):
    try:
        with gzip.open(p, "rt", errors="replace") as f: return f.read()
    except FileNotFoundError: return ""
    except Exception as e: return f"__READ_ERROR__ {e}"

def classify(rel):
    r = rel.replace("\\", "/")
    for key, exp in (("e1-corpus/", "E1"), ("e4-pressure/", "E4"), ("e5-optimizer/", "E5"), ("e3-security/", "E3"), ("e2-bench/", "E2")):
        if key in r: return exp
    if "corpus-raw" in r or r.startswith("corpus/"): return "E1(work-copy)"
    if r.startswith("infra-failed"): return "E1(infra-failed)"
    if r.startswith("e6/"): return "E6(canary,not audited)"
    if r.startswith("canary-audit-rerun"): return "canary-rerun"
    if r.startswith("canary"): return "canary"
    if "trials/pilot" in r: return "harness-pilot"
    if "trials/e4" in r: return "E4(work-copy)"
    if "trials/e5" in r: return "E5(work-copy)"
    return "unclassified"

def kind(rel):
    r = rel
    if "runs-pilot" in r or "trials/pilot" in r or "-pilot" in r or "/pilot" in r: return "pilot"
    if "canary" in r: return "canary"
    if "websearch" in r: return "websearch-variant"
    if "infra-failed" in r: return "infra-failed"
    return "main"

def assistant_text(tool, txt):
    out = []
    for ln in txt.splitlines():
        try: e = json.loads(ln)
        except Exception: continue
        if tool == "claude" and e.get("type") == "assistant":
            for b in (e.get("message") or {}).get("content") or []:
                if isinstance(b, dict) and b.get("type") == "text": out.append(b.get("text") or "")
        elif tool == "codex":
            it = e.get("item") or {}
            if e.get("type") == "item.completed" and it.get("type") == "agent_message": out.append(it.get("text") or "")
        elif tool == "agy" and e.get("event") == "result": out.append(((e.get("result") or {}).get("response")) or "")
        elif tool == "opencode" and e.get("type") == "text": out.append((e.get("part") or {}).get("text") or "")
    return "\n".join(out)

def init_info(txt):
    for ln in txt.splitlines()[:40]:
        if '"subtype":"init"' in ln.replace(" ", "") or '"subtype": "init"' in ln:
            try: e = json.loads(ln)
            except Exception: continue
            if e.get("type") == "system" and e.get("subtype") == "init": return e
    return None

def hook_events(txt):
    n = ctx = 0; names = set()
    for ln in txt.splitlines():
        if '"hook_' not in ln: continue
        try: e = json.loads(ln)
        except Exception: continue
        if e.get("type") == "system" and str(e.get("subtype", "")).startswith("hook_"):
            n += 1; names.add(e.get("hook_name") or e.get("hook_event") or "?")
            s = json.dumps(e)
            if "additionalContext" in s or "additional_context" in s: ctx += 1
    return n, ctx, sorted(names)

roots = [(STUDY, "R"), (WORK, "W")]
rundirs = {}
for root, tag in roots:
    for dp, dn, fn in os.walk(root):
        if "node_modules" in dp or "/.git" in dp: continue
        if "transcript.jsonl.gz" in fn or "transcript_full.jsonl.gz" in fn:
            rundirs[(tag, dp)] = fn
rows, hits = [], []
for (tag, dp), fn in sorted(rundirs.items()):
    root = STUDY if tag == "R" else WORK
    rel = os.path.relpath(dp, root); rid = f"{tag}:{rel}"
    meta = {}
    try: meta = json.loads(pathlib.Path(dp, "meta.json").read_text())
    except Exception: pass
    tool = meta.get("tool") or next((t for t in ("claude", "codex", "agy", "opencode", "gemini") if t in rel.lower()), "?")
    t1, t2, se = rd(dp + "/transcript.jsonl.gz"), rd(dp + "/transcript_full.jsonl.gz"), rd(dp + "/stderr.txt.gz")
    init = init_info(t1) if tool == "claude" else None
    plug = [p.get("name") for p in (init or {}).get("plugins", [])] if init else None
    hn, hc, hnames = hook_events(t1) if tool == "claude" else (0, 0, [])
    cnt = collections.Counter(); icnt = collections.Counter()
    for label, body in (("transcript", t1), ("transcript_full", t2), ("stderr", se)):
        b = INIT_STRIP.sub('"capabilities"', body); b = MEM_STRIP.sub("", b)
        for k, rx in LEAK_RE.items():
            ms = list(rx.finditer(b)); cnt[k] += len(ms)
            for m in ms[:2]:
                hits.append((rid, label, k, b[max(0, m.start() - 70):m.end() + 70].replace("\n", " ")))
        for k, rx in INFO_RE.items(): icnt[k] += len(rx.findall(b))
    at = assistant_text(tool, t1)
    words = WORD.findall(at); art = len(ART.findall(at))
    rows.append({
        "run_id": rid, "experiment": classify(rel), "kind": kind(rel), "tool": tool,
        "markers_found": ";".join(f"{k}:{v}" for k, v in sorted(cnt.items()) if v) or "none",
        "strong_marker_total": sum(v for k, v in cnt.items() if k not in ("factory(weak)", "homepath")),
        "plugins_loaded": "|".join(plug) if plug is not None else "n/a",
        "mcp_servers": len((init or {}).get("mcp_servers", [])) if init else "n/a",
        "slash_commands": len((init or {}).get("slash_commands", [])) if init else "n/a",
        "skills": len((init or {}).get("skills", [])) if init else "n/a",
        "claude_tools": "|".join((init or {}).get("tools", [])) if init else "n/a",
        "hook_events": hn, "hook_additionalContext": hc, "hook_names": "|".join(hnames),
        "config_hash": meta.get("harness_config_hash") or "none", "real_home": meta.get("real_home", ""), "canary_planted": meta.get("canary_planted", ""),
        "cli_version": meta.get("cli_version", ""), "model_reported": meta.get("model_reported") or meta.get("model_arg") or "",
        "transcript_bytes": len(t1), "transcript_full_bytes": len(t2), "asst_words": len(words), "article_per100w": round(100 * art / len(words), 2) if words else "",
        "opens_with_Cleanse": bool(at.lstrip().startswith("Cleanse")), "info": ";".join(f"{k}:{v}" for k, v in sorted(icnt.items()) if v) or "-",
        "has_meta": bool(meta), "read_error": "__READ_ERROR__" in (t1 + t2 + se),
    })
cols = list(rows[0].keys())
with open(HERE / "transcript_scan.csv", "w", newline="") as f:
    w = csv.DictWriter(f, fieldnames=cols); w.writeheader(); w.writerows(rows)
with open(HERE / "marker_hits.tsv", "w") as f:
    f.write("run_id\tfile\tmarker\tcontext\n")
    for h in hits: f.write("\t".join(h) + "\n")
print(len(rows), "run dirs;", len(hits), "hit snippets")
