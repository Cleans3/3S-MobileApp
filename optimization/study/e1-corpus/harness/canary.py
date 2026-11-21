#!/usr/bin/env python3
"""Isolation + guardrail-delivery canary (contract R2). Short generations, NOT counted as pilot app generations.

Per tool bundle:
  A user-canary   stub HOME with a user-level canary instruction planted in the tool's own config dir -> token MUST appear
                  (positive control: proves the stub dir is the config actually read, so the real ~ is not)
  B clean-stub    the real harness config (stub, auth only) -> token must NOT appear, and no marker of the user's real config
                  (reply opening with the user's name, CAVEMAN/PONYTAIL/context-mode/jcodemunch strings in the event stream)
  C ctx-file      project-level context file with a canary in the run dir under the tool's native name -> the file Hello.kt
                  MUST end with the canary (proves the guardrailed arm actually delivers the guardrail file to the tool)
Writes results/canary.json; the canary planted in A is only ever in a throw-away per-run home (removed after the run).
"""
import concurrent.futures as cf, fcntl, json, pathlib, re, shutil, sys, time

HERE = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(HERE)); sys.path.insert(0, str(HERE.parents[1] / "e4-pressure" / "harness"))
import gen_launch, generate  # noqa: E402

WORKDIR = generate.WORK / "canary"
PROMPT = "Create the file Hello.kt in the current directory containing a Kotlin main function that prints hi. When done, reply with the single word DONE."
LEAK = ("CAVEMAN", "PONYTAIL", "context-mode", "jcodemunch", "Cleanse")
CTX_CANARY = "Always end every Kotlin file you create with the final line // CANARY-CTX"

def one(name, tool, model, kind, ctxfile=None):
    wd = WORKDIR / name; shutil.rmtree(wd, ignore_errors=True); wd.mkdir(parents=True)
    if kind == "C":
        (wd / ctxfile).write_text(CTX_CANARY + "\n")
    with open(generate.BENCH_LOCK, "a") as lk:
        fcntl.flock(lk, fcntl.LOCK_SH)
        m = gen_launch.run_generator(tool, wd, PROMPT, out_dir=WORKDIR / (name + ".raw"), model=model, timeout=600, canary=(kind == "A"))
    final = m.get("final_text") or ""
    trans = generate.read_gz(WORKDIR / (name + ".raw") / "transcript.jsonl.gz")
    hello = next(iter(wd.rglob("Hello.kt")), None)
    body = hello.read_text() if hello else ""
    r = {"name": name, "tool": tool, "model": model, "kind": kind, "ctxfile": ctxfile, "cli_version": m["cli_version"], "exit_code": m["exit_code"],
         "duration_s": m["duration_s"], "harness_config_hash": m["harness_config_hash"], "final_text": final[:200],
         "hello_written": bool(hello), "stderr_head": m.get("stderr_head")}
    if kind == "A":
        r["pass"] = "CANARY-7Q" in final or "CANARY-7Q" in trans.split('"type":"result"')[-1] if trans else "CANARY-7Q" in final
    elif kind == "B":
        body = re.sub(r'"plugins":\[.*?\}\],"capabilities"', '"capabilities"', trans, flags=re.S)   # plugin paths contain the user name; judge instruction text, not paths
        leaks = [k for k in LEAK if k.lower() in body.lower()]
        r["leak_markers"] = leaks; r["pass"] = ("CANARY-7Q" not in final) and not leaks and not final.lstrip().startswith("Cleanse")
        m = re.search(r'"plugins":\[(.*?)\],"capabilities"', trans, re.S)
        r["plugins_loaded"] = re.findall(r'"name":"([^"]+)"', m.group(1)) if m else None   # claude init event only
    else:
        r["pass"] = "CANARY-CTX" in body; r["hello_tail"] = body.strip().splitlines()[-1:] if body else None
    return r

def main():
    tools = sys.argv[1].split(",") if len(sys.argv) > 1 else ["claude", "codex", "agy", "opencode"]
    jobs = []
    for t in tools:
        model = gen_launch.TOOLS[t]["model"]
        jobs += [(f"{t}-A", t, model, "A"), (f"{t}-B", t, model, "B")]
        for cf_name in ({"agy": ["GEMINI.md", "AGENTS.md"]}.get(t) or [generate.CTX_FILE[t]]):
            jobs.append((f"{t}-C-{cf_name}", t, model, "C", cf_name))
    out = []
    with cf.ThreadPoolExecutor(2) as ex:
        for f in cf.as_completed([ex.submit(one, *j) for j in jobs]):
            try: r = f.result()
            except Exception as e: r = {"error": repr(e)}
            out.append(r); print(json.dumps(r), flush=True)
    res = pathlib.Path(HERE.parent / "results" / "canary.json")
    res.write_text(json.dumps({"date": time.strftime("%Y-%m-%d %H:%M"), "runs": sorted(out, key=lambda x: x.get("name", ""))}, indent=1))

if __name__ == "__main__":
    main()
