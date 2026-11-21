#!/usr/bin/env python3
"""Harness smoke test (not a corpus run): does agy survive housekeeping + toolchain-probe commands under the E1 allow-list,
and is curl still denied? Writes results/smoke_agy.json."""
import fcntl, json, pathlib, shutil, sys
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import generate as G

PROMPT = ("Work in the current empty directory. Do these in order and do not ask me questions: "
          "1) create notes.txt containing the word ok; 2) run: mkdir -p a/b; 3) run: java -version; "
          "4) run: chmod +x notes.txt; 5) create a/b/done.txt containing the word done; "
          "6) run: curl -s -o /dev/null https://example.com . Report nothing else.")
wd = G.WORK / "corpus" / "smoke-agy"
raw = G.WORK / "corpus-raw" / "smoke-agy"
for d in (wd, raw):
    shutil.rmtree(d, ignore_errors=True); d.mkdir(parents=True)
with open(G.BENCH_LOCK, "a") as lk:
    fcntl.flock(lk, fcntl.LOCK_SH)
    meta = G.gen_launch.run_generator("agy", wd, PROMPT, out_dir=raw, model="gemini-3.8-flash-medium", timeout=600)
import gzip
t = gzip.open(raw / "transcript.jsonl.gz", "rt", errors="replace").read()
cmds = []
for line in t.splitlines():
    try: G._cmds(json.loads(line), cmds)
    except ValueError: pass
files = sorted(str(p.relative_to(wd)) for p in wd.rglob("*") if p.is_file())
out = {"exit_code": meta["exit_code"], "files": files, "commands": cmds, "denied_actions": G.denied_actions(t),
       "net_audit": G.net_audit(t), "stderr_head": meta.get("stderr_head")}
(G.E1 / "results" / "smoke_agy.json").write_text(json.dumps(out, indent=1))
print(json.dumps(out, indent=1))
