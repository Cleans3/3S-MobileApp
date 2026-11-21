#!/usr/bin/env python3
"""A-variant candidates for the corpus-derived E2 factors (protocol A4, rule in a-selection-rule.txt).

  python3 harness/a_candidates.py

For each factor, candidates = campaign runs (never pilots) with a detector finding for the factor's rule ID, ordered by run START time
(generated_at - duration_s; duration includes any lock wait). Output: results/a_candidates.csv and results/a_candidates.md (excerpts).
'port_hint' is a hint, not a verdict: the number of imports in the flagged file that come from the app's own package (each is a thing
the optlab screen would have to bind), so lower is closer to a mechanical port. Porting and the port diff belong to the E2 owner.
"""
import collections, csv, datetime, pathlib

E1 = pathlib.Path(__file__).resolve().parents[1]
RES = E1 / "results"
FACTORS = {"P08": ["P08"], "P06": ["P06"], "P01+P09": ["P01", "P09"], "P13": ["P13"], "P03+P04": ["P03", "P04"]}


def start(r):
    t = datetime.datetime.strptime(r["generated_at"], "%Y-%m-%dT%H:%M:%S%z")
    return t - datetime.timedelta(seconds=float(r["duration_s"] or 0))


def main():
    runs = {r["run_id"]: r for r in csv.DictReader(open(RES / "runs.csv")) if r["pilot"] != "True" and r["valid"] == "True"}
    fnd = [f for f in csv.DictReader(open(RES / "findings.csv")) if f["run_id"] in runs and f["detector"] == "slop-scan" and f["triage"] != "fp"]
    rows, md = [], []
    done = len(runs)
    md.append(f"# A-variant candidates (protocol A4)\n\nCampaign valid runs on disk: {done} of 108 (50% mark = 54). Order = run start time. port_hint = imports from the app's own package in the flagged file (lower is closer to a mechanical port).\n")
    for factor, rules in FACTORS.items():
        grp = collections.defaultdict(list)                               # (run, file) -> findings
        for f in fnd:
            if f["rule_id"] in rules: grp[(f["run_id"], f["file"])].append(f)
        cands = sorted(grp.items(), key=lambda kv: (start(runs[kv[0][0]]), kv[0][1]))
        md.append(f"\n## {factor}: {len(cands)} candidate file(s) in {len({k[0] for k, _ in cands})} run(s)\n")
        for rank, ((rid, file), fs) in enumerate(cands, 1):
            p = E1 / "runs" / rid / "src" / file
            text = p.read_text(errors="replace") if p.exists() else ""
            lines = text.splitlines()
            pkg = next((l.split()[1].rstrip(";") for l in lines if l.startswith("package ")), "")
            root = ".".join(pkg.split(".")[:3])
            own = sum(1 for l in lines if l.startswith("import " + root + ".")) if root else 0
            rr = runs[rid]
            rules_here = sorted({f["rule_id"] for f in fs})
            rows.append({"factor": factor, "rank": rank, "run_id": rid, "bundle": rr["bundle"], "style": rr["prompt_style"], "start": start(rr).isoformat(timespec="seconds"),
                         "rules": "+".join(rules_here), "both_rules_in_file": int(len(rules_here) == len(rules)), "file": file,
                         "lines": " ".join(sorted({f["line"] for f in fs}, key=int)), "file_loc": len(lines), "port_hint": own, "evidence": fs[0]["evidence"][:140]})
            if rank <= 4:
                ln = int(fs[0]["line"]) if fs[0]["line"].isdigit() else 1
                a, b = max(0, ln - 7), min(len(lines), ln + 6)
                ex = "\n".join(f"{i + 1:4d}{'>' if i + 1 == ln else ' '} {lines[i][:160]}" for i in range(a, b))
                md.append(f"### {factor} #{rank}: {rid} {file}:{rows[-1]['lines']} ({'+'.join(rules_here)}, port_hint {own})\n{fs[0]['evidence'][:140]}\n\n" + "~~~kotlin\n" + ex + "\n~~~\n")
    with open(RES / "a_candidates.csv", "w", newline="") as f:
        w = csv.DictWriter(f, ["factor", "rank", "run_id", "bundle", "style", "start", "rules", "both_rules_in_file", "file", "lines", "file_loc", "port_hint", "evidence"])
        w.writeheader(); w.writerows(rows)
    (RES / "a_candidates.md").write_text("\n".join(md))
    c = collections.Counter(r["factor"] for r in rows)
    print(f"{len(rows)} candidate files over {done} valid runs:", dict(c))


if __name__ == "__main__":
    main()
