#!/usr/bin/env python3
"""Re-run of the E1 isolation canary (e1-corpus/harness/canary.py, imported unchanged) for the audit.
Differences vs canary.py: work dir is a NEW dir (the original canary evidence is not overwritten), result goes to isolation-audit/canary_rerun.json,
no pyc written. Same prompt, same A/B/C kinds, same stub HOME + launcher as the generation runs."""
import sys, json, time, pathlib, concurrent.futures as cf
sys.dont_write_bytecode = True
HERE = pathlib.Path(__file__).resolve().parent
E1H = HERE.parent / "e1-corpus" / "harness"
sys.path.insert(0, str(E1H)); sys.path.insert(0, str(HERE.parent / "e4-pressure" / "harness"))
import canary, generate, gen_launch
canary.WORKDIR = generate.WORK / "canary-audit-rerun-2026-11-27"
tools = sys.argv[1].split(",") if len(sys.argv) > 1 else ["claude", "codex", "agy", "opencode"]
jobs = []
for t in tools:
    model = gen_launch.TOOLS[t]["model"]
    jobs += [(f"{t}-A", t, model, "A"), (f"{t}-B", t, model, "B")]
    for cfn in ({"agy": ["GEMINI.md", "AGENTS.md"]}.get(t) or [generate.CTX_FILE[t]]):
        jobs.append((f"{t}-C-{cfn}", t, model, "C", cfn))
out = []
with cf.ThreadPoolExecutor(2) as ex:
    futs = {ex.submit(canary.one, *j): j for j in jobs}
    for f in cf.as_completed(futs):
        try: r = f.result()
        except Exception as e: r = {"name": futs[f][0], "error": repr(e)}
        out.append(r); print(json.dumps(r), flush=True)
# stub-only facts for the record
res = {"date": time.strftime("%Y-%m-%d %H:%M"), "stub_config_hash_now": gen_launch.config_hash(), "workdir": str(canary.WORKDIR),
       "cli_versions": {t: gen_launch.version(t) for t in tools},
       "runs": sorted(out, key=lambda x: x.get("name", ""))}
(HERE / "canary_rerun.json").write_text(json.dumps(res, indent=1))
print("PASS" if all(r.get("pass") for r in out) else "NOT-ALL-PASS", sum(1 for r in out if r.get("pass")), "/", len(out))
