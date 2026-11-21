#!/usr/bin/env python3
"""Build the blinded precision sample (protocol 5b, R6b) for the second rater.

  blind_sample.py [--per-rule 6] [--seed 20261121]

Stratified by rule x arm: for every measured rule, up to --per-rule findings, spread evenly over arms {naive, detailed-functional, guardrailed, human}.
Outputs (results/validation/):
  blind_sample.csv      sample_id, rule_id, rule_definition, file_tail, line, evidence, context  (NO run_id, tool, style, repo: salted hash only)
  blind_key.csv         sample_id -> run_id, arm, bundle, file, line   (never shown to the rater)
  rating_sheet.csv      sample_id, rater, label(tp|fp|unclear), note    (empty; rater fills)
Context = the flagged line +-6 lines for Kotlin, the surrounding XML/Gradle lines otherwise. depcheck/lint/detekt findings are not sampled (their evidence is mechanical).
"""
import argparse, collections, csv, hashlib, json, pathlib, random, sys

E1 = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(E1 / "slop-scan"))
import slopscan, r_perf, r_sec, r_ui, r_stale  # noqa: E402,F401
WORK = pathlib.Path("/mnt/01DC198BE9192500/github/Schooldev/3S-study-work")
DEFS = {fn.ids[0]: (fn.__doc__ or "").strip() for fn in slopscan.RULES}

def context(path, line, n=6):
    try: ls = pathlib.Path(path).read_text(errors="replace").splitlines()
    except OSError: return "(source missing)"
    a = max(0, int(line) - 1 - n); b = min(len(ls), int(line) + n)
    return "\n".join(f"{i + 1:4d}{'>' if i + 1 == int(line) else ' '} {ls[i]}" for i in range(a, b))

def main():
    ap = argparse.ArgumentParser(); ap.add_argument("--per-rule", type=int, default=6); ap.add_argument("--seed", type=int, default=20261121); a = ap.parse_args()
    rng = random.Random(a.seed); res = E1 / "results"
    runs = {r["run_id"]: r for r in csv.DictReader(open(res / "runs.csv")) if not r["run_id"].startswith("pilot-")}
    pool = collections.defaultdict(lambda: collections.defaultdict(list))   # rule -> arm -> findings
    for f in csv.DictReader(open(res / "findings.csv")):
        if f["detector"] == "slop-scan" and f["run_id"] in runs and f["rule_id"] in DEFS and f["line"].isdigit():
            f["src"] = str(E1 / "runs" / f["run_id"] / "src" / f["file"]); f["arm"] = runs[f["run_id"]]["prompt_style"]; f["bundle"] = runs[f["run_id"]]["bundle"]
            pool[f["rule_id"]][f["arm"]].append(f)
    hc = {("human:" + h["repo"] + "@" + h["commit"][:8]): h for h in csv.DictReader(open(res / "human_corpus.csv"))} if (res / "human_corpus.csv").exists() else {}
    if (res / "human_findings.csv").exists():
        for f in csv.DictReader(open(res / "human_findings.csv")):
            h = hc.get(f["run_id"])
            if h and f["detector"] == "slop-scan" and f["rule_id"] in DEFS and f["line"].isdigit():
                f["src"] = str(WORK / "human-corpus" / h["repo"].replace("/", "__") / f["file"]); f["arm"] = "human"; f["bundle"] = h["stratum"]
                pool[f["rule_id"]]["human"].append(f)
    sample = []
    for rule in sorted(pool):
        arms = sorted(pool[rule]); chosen = []
        for arm in arms: rng.shuffle(pool[rule][arm])
        i = 0
        while len(chosen) < a.per_rule and any(pool[rule][arm] for arm in arms):          # round-robin over arms
            arm = arms[i % len(arms)]; i += 1
            if pool[rule][arm]:
                f = pool[rule][arm].pop()
                if (f["run_id"], f["file"], f["line"]) not in {(c["run_id"], c["file"], c["line"]) for c in chosen}: chosen.append(f)
        sample += chosen
    rng.shuffle(sample)
    salt = str(a.seed)
    vd = res / "validation"; vd.mkdir(exist_ok=True)
    with open(vd / "blind_sample.csv", "w", newline="") as fs, open(vd / "blind_key.csv", "w", newline="") as fk, open(vd / "rating_sheet.csv", "w", newline="") as fr:
        ws = csv.writer(fs); wk = csv.writer(fk); wr = csv.writer(fr)
        ws.writerow(["sample_id", "rule_id", "rule_definition", "file_tail", "line", "evidence", "context"]); wk.writerow(["sample_id", "run_id", "arm", "bundle", "file", "line"]); wr.writerow(["sample_id", "rater", "label", "note"])
        for f in sample:
            sid = hashlib.sha256((salt + f["run_id"] + f["file"] + f["line"] + f["rule_id"]).encode()).hexdigest()[:10]
            ws.writerow([sid, f["rule_id"], DEFS[f["rule_id"]], "/".join(f["file"].split("/")[-2:]), f["line"], f["evidence"], context(f["src"], f["line"])])
            wk.writerow([sid, f["run_id"], f["arm"], f["bundle"], f["file"], f["line"]])
    cnt = collections.Counter(f["rule_id"] for f in sample)
    print(len(sample), "sampled;", {r: cnt[r] for r in sorted(cnt)}, "rules with < 5:", sorted(r for r in DEFS if cnt[r] < 5))

if __name__ == "__main__":
    main()
