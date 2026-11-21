#!/usr/bin/env python3
"""Supplementary blinded precision sample v2 (alpha, A13): FRESH items only, drawn from the v2 (fixed) detector's findings.

  blind_sample_v2.py [--per-rule 6] [--seed 20261121] [--rules P10,S03,P05,P09,P07,U05]

Rules = the five fixed rules plus U05 (whole-file context: its findings are file-level and v1 items were unjudgeable from a +-6 line window).
Excludes every (run_id, file, line, rule) already in results/validation/v1/blind_key.csv, so a fix is judged out of sample.
Context window +-12 lines for every item; U05 shows the whole file (first 250 lines). Stratified by arm like v1 (round-robin over arms).
Outputs results/validation/v2/{blind_sample.csv, blind_key.csv, rating_sheet_r1_template.csv}; the pack for r2 is copied to the work volume by the caller.
"""
import argparse, collections, csv, hashlib, pathlib, random, sys

E1 = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(E1 / "harness"))
import blind_sample as bs  # noqa: E402  (DEFS, WORK; read-only reuse)

def context(path, line, n=12, whole=False):
    try: ls = pathlib.Path(path).read_text(errors="replace").splitlines()
    except OSError: return "(source missing)"
    if whole: a, b = 0, min(len(ls), 250)
    else: a = max(0, int(line) - 1 - n); b = min(len(ls), int(line) + n)
    return "\n".join(f"{i + 1:4d}{'>' if i + 1 == int(line) else ' '} {ls[i]}" for i in range(a, b))

def main():
    ap = argparse.ArgumentParser(); ap.add_argument("--per-rule", type=int, default=6); ap.add_argument("--seed", type=int, default=20261121)
    ap.add_argument("--rules", default="P10,S03,P05,P09,P07,U05"); a = ap.parse_args()
    rules = a.rules.split(","); rng = random.Random(a.seed + 2); res = E1 / "results"
    v1 = {(k["run_id"], k["file"], k["line"]) for k in csv.DictReader(open(res / "validation" / "v1" / "blind_key.csv"))}
    runs = {r["run_id"]: r for r in csv.DictReader(open(res / "runs.csv")) if not r["run_id"].startswith("pilot-")}
    pool = collections.defaultdict(lambda: collections.defaultdict(list))
    for f in csv.DictReader(open(res / "findings.csv")):
        if f["detector"] == "slop-scan" and f["rule_id"] in rules and f["run_id"] in runs and f["line"].isdigit() and (f["run_id"], f["file"], f["line"]) not in v1:
            f["src"] = str(E1 / "runs" / f["run_id"] / "src" / f["file"]); f["arm"] = runs[f["run_id"]]["prompt_style"]; f["bundle"] = runs[f["run_id"]]["bundle"]
            pool[f["rule_id"]][f["arm"]].append(f)
    hc = {("human:" + h["repo"] + "@" + h["commit"][:8]): h for h in csv.DictReader(open(res / "human_corpus.csv"))}
    for f in csv.DictReader(open(res / "human_findings.csv")):
        h = hc.get(f["run_id"])
        if h and f["detector"] == "slop-scan" and f["rule_id"] in rules and f["line"].isdigit() and (f["run_id"], f["file"], f["line"]) not in v1:
            f["src"] = str(bs.WORK / "human-corpus" / h["repo"].replace("/", "__") / f["file"]); f["arm"] = "human"; f["bundle"] = h["stratum"]
            pool[f["rule_id"]]["human"].append(f)
    sample = []
    for rule in rules:
        arms = sorted(pool[rule]); chosen = []; seen = set()
        for arm in arms: rng.shuffle(pool[rule][arm])
        i = 0
        while len(chosen) < a.per_rule and any(pool[rule][arm] for arm in arms):
            arm = arms[i % len(arms)]; i += 1
            if pool[rule][arm]:
                f = pool[rule][arm].pop(); key = (f["run_id"], f["file"], f["line"])
                if key not in seen: seen.add(key); chosen.append(f)
        sample += chosen
    rng.shuffle(sample)
    out = res / "validation" / "v2"; out.mkdir(parents=True, exist_ok=True); salt = str(a.seed) + "v2"
    with open(out / "blind_sample.csv", "w", newline="") as fs, open(out / "blind_key.csv", "w", newline="") as fk, open(out / "rating_sheet_r1_template.csv", "w", newline="") as fr:
        ws, wk, wr = csv.writer(fs), csv.writer(fk), csv.writer(fr)
        ws.writerow(["sample_id", "rule_id", "rule_definition", "file_tail", "line", "evidence", "context"]); wk.writerow(["sample_id", "run_id", "arm", "bundle", "file", "line"]); wr.writerow(["sample_id", "rater", "label", "note"])
        for f in sample:
            sid = hashlib.sha256((salt + f["run_id"] + f["file"] + f["line"] + f["rule_id"]).encode()).hexdigest()[:10]
            ws.writerow([sid, f["rule_id"], bs.DEFS[f["rule_id"]], "/".join(f["file"].split("/")[-2:]), f["line"], f["evidence"], context(f["src"], f["line"], whole=(f["rule_id"] == "U05"))])
            wk.writerow([sid, f["run_id"], f["arm"], f["bundle"], f["file"], f["line"]])
    cnt = collections.Counter(f["rule_id"] for f in sample)
    print(len(sample), "sampled;", {r: cnt[r] for r in rules}, "pool sizes after v1 exclusion:", {r: sum(len(v) for v in pool[r].values()) + cnt[r] for r in rules})

if __name__ == "__main__":
    main()
