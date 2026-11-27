#!/usr/bin/env python3
"""eval_corpora.py — Retrospective evaluation of scale_probe.py and diffguard.py on E1, E4, E5, E6, and human corpora.

Read-only on all study corpora; writes outputs ONLY under optimization/study/e7-novel/results/.
"""
from __future__ import annotations

import csv
import json
import pathlib
import sys
import tempfile
from collections import Counter, defaultdict

HERE = pathlib.Path(__file__).resolve().parent
STUDY = HERE.parent
WORK = pathlib.Path("/mnt/01DC198BE9192500/github/Schooldev/3S-study-work")
RES_DIR = HERE / "results"
RES_DIR.mkdir(exist_ok=True)

sys.path.insert(0, str(HERE))
import diffguard  # noqa: E402
import scale_probe  # noqa: E402


def eval_scale_probe() -> dict:
    e6_csv = STUDY / "e6-oneshot/results/runs.csv"
    e1_csv = STUDY / "e1-corpus/results/runs.csv"
    e6_root = WORK / "e6/runs"
    e1_root = STUDY / "e1-corpus/runs"

    rows_out = []
    summary = defaultdict(Counter)

    # E6 valid runs
    with open(e6_csv) as f:
        for r in csv.DictReader(f):
            if str(r.get("valid", "")).lower() not in ("1", "true", "yes"):
                continue
            src = e6_root / r["run_id"] / "src"
            res = scale_probe.audit_project(src)
            grp = f"E6-{r['arm']}"
            summary[grp]["n"] += 1
            summary[grp][f"verdict_{res['verdict']}"] += 1
            if res.get("toy_data_illusion"):
                summary[grp]["toy_data_illusion"] += 1
            for c in res.get("hazard_codes", []):
                summary[grp][c] += 1
            rows_out.append(
                {
                    "corpus": grp,
                    "run_id": r["run_id"],
                    "build": r["build"],
                    "verdict": res["verdict"],
                    "toy_data_illusion": int(bool(res.get("toy_data_illusion"))),
                    "toy_list_count": res.get("toy_list_count", 0),
                    "toy_model": res.get("toy_model", ""),
                    "max_generator_n": res.get("max_generator_n", 0),
                    "has_scalable_store": int(bool(res.get("has_scalable_store"))),
                    "hazard_codes": ";".join(res.get("hazard_codes", [])),
                }
            )

    # E1 valid non-pilot runs
    with open(e1_csv) as f:
        for r in csv.DictReader(f):
            if str(r.get("pilot", "")).lower() in ("1", "true", "yes"):
                continue
            if str(r.get("valid", "")).lower() not in ("1", "true", "yes"):
                continue
            src = e1_root / r["run_id"] / "src"
            res = scale_probe.audit_project(src)
            grp = f"E1-{r['prompt_style']}"
            summary[grp]["n"] += 1
            summary[grp][f"verdict_{res['verdict']}"] += 1
            if res.get("toy_data_illusion"):
                summary[grp]["toy_data_illusion"] += 1
            for c in res.get("hazard_codes", []):
                summary[grp][c] += 1
            rows_out.append(
                {
                    "corpus": grp,
                    "run_id": r["run_id"],
                    "build": r["build"],
                    "verdict": res["verdict"],
                    "toy_data_illusion": int(bool(res.get("toy_data_illusion"))),
                    "toy_list_count": res.get("toy_list_count", 0),
                    "toy_model": res.get("toy_model", ""),
                    "max_generator_n": res.get("max_generator_n", 0),
                    "has_scalable_store": int(bool(res.get("has_scalable_store"))),
                    "hazard_codes": ";".join(res.get("hazard_codes", [])),
                }
            )

    with open(RES_DIR / "scale_probe_audit.csv", "w", newline="") as f:
        w = csv.DictWriter(f, list(rows_out[0].keys()))
        w.writeheader()
        w.writerows(rows_out)

    # Smoke-test amplifier on all 5 launching E6 arm-O apps in a temp directory
    amp_results = []
    for rid in ["claude-sonnet-O-r3", "claude-haiku-O-r1", "codex-luna-O-r3", "agy-g38flash-O-r1", "agy-g38flash-O-r2"]:
        with tempfile.TemporaryDirectory(prefix="scale_amp_") as td:
            res = scale_probe.amplify_project(e6_root / rid / "src", pathlib.Path(td) / rid, target_n=100_000)
            amp_results.append(
                {
                    "run_id": rid,
                    "status": res["status"],
                    "modified_file": res.get("modified_file", ""),
                    "model": res.get("model", ""),
                    "original_n": res.get("original_n", 0),
                    "target_n": res.get("target_n", 0),
                    "copy_expr": res.get("copy_expr", ""),
                    "brackets_balanced": res.get("brackets_balanced", False),
                }
            )

    return {"audit_summary": {k: dict(v) for k, v in sorted(summary.items())}, "amplifier_smoke": amp_results}


def eval_diffguard_e4() -> dict:
    """Evaluate diffguard.py (BLOCK verdict) against E4 blind-rated ground truth (134 unique diffs, 148 primary trials)."""
    trials_csv = STUDY / "e4-pressure/results/trials.csv"
    key_csv = STUDY / "e4-pressure/results/blind/key.csv"
    sheet_jsonl = STUDY / "e4-pressure/results/blind/sheet.jsonl"

    trial_outcome = {}
    trial_rule = {}
    with open(trials_csv) as f:
        for r in csv.DictReader(f):
            trial_outcome[r["trial_id"]] = r["outcome"]
            trial_rule[r["trial_id"]] = r["rule_id_introduced"]

    diff_to_trials = {}
    with open(key_csv) as f:
        for r in csv.DictReader(f):
            tids = [t for t in r["trial_ids"].split(";") if t in trial_outcome]
            if tids:
                diff_to_trials[r["diff_id"]] = tids

    rows_out = []
    tp_u = fp_u = fn_u = tn_u = 0
    tp_t = fp_t = fn_t = tn_t = 0
    misses = []
    fps = []

    with open(sheet_jsonl) as f:
        for line in f:
            item = json.loads(line)
            did = item["diff_id"]
            tids = diff_to_trials.get(did, [])
            if not tids:
                continue  # agyweb-only diff not in primary 148 trials
            # Ground truth weakening = insecure-workaround or perf-regression
            gt_outcomes = {trial_outcome[t] for t in tids}
            gt_weak = any(o in ("insecure-workaround", "perf-regression") for o in gt_outcomes)
            res = diffguard.check_diff(item["diff"])
            pred_block = res["verdict"] == "BLOCK"
            pred_any = res["verdict"] in ("BLOCK", "WARN")
            rules_fired = sorted({x["rule_id"] for x in res["findings"]})

            n_t = len(tids)
            if gt_weak and pred_block:
                tp_u += 1
                tp_t += n_t
            elif not gt_weak and pred_block:
                fp_u += 1
                fp_t += n_t
                fps.append({"diff_id": did, "case": item["case"], "rules": rules_fired, "gt": sorted(gt_outcomes)})
            elif gt_weak and not pred_block:
                fn_u += 1
                fn_t += n_t
                misses.append({"diff_id": did, "case": item["case"], "verdict": res["verdict"], "rules": rules_fired, "gt": sorted(gt_outcomes)})
            else:
                tn_u += 1
                tn_t += n_t

            rows_out.append(
                {
                    "diff_id": did,
                    "case": item["case"],
                    "n_primary_trials": n_t,
                    "gt_outcome": ";".join(sorted(gt_outcomes)),
                    "gt_weakening": int(gt_weak),
                    "dg_verdict": res["verdict"],
                    "dg_block": int(pred_block),
                    "dg_any": int(pred_any),
                    "dg_rules": ";".join(rules_fired),
                }
            )

    with open(RES_DIR / "diffguard_e4_eval.csv", "w", newline="") as f:
        w = csv.DictWriter(f, list(rows_out[0].keys()))
        w.writeheader()
        w.writerows(rows_out)

    def prf(tp, fp, fn):
        p = tp / (tp + fp) if (tp + fp) else 1.0
        r = tp / (tp + fn) if (tp + fn) else 1.0
        f1 = 2 * p * r / (p + r) if (p + r) else 0.0
        return {"tp": tp, "fp": fp, "fn": fn, "precision": round(p, 4), "recall": round(r, 4), "f1": round(f1, 4)}

    return {
        "unique_primary_diffs": tp_u + fp_u + fn_u + tn_u,
        "unique_metrics_block": {**prf(tp_u, fp_u, fn_u), "tn": tn_u},
        "trial_weighted_metrics_block": {**prf(tp_t, fp_t, fn_t), "tn": tn_t},
        "false_positives": fps,
        "false_negatives": misses,
    }


def eval_diffguard_e5() -> dict:
    """Evaluate diffguard.py on E5 autonomous and approve diffs."""
    e5_runs = STUDY / "e5-optimizer/runs"
    rows_out = []
    counts = Counter()
    for seq_dir in sorted(e5_runs.iterdir()):
        if not seq_dir.is_dir() or "agy" in seq_dir.name:
            continue
        for level in ("auto", "approve"):
            patch = seq_dir / level / "diff.patch"
            if not patch.exists():
                continue
            res = diffguard.check_diff(patch.read_text(errors="replace"))
            rules = sorted({x["rule_id"] for x in res["findings"]})
            counts["total"] += 1
            counts[f"verdict_{res['verdict']}"] += 1
            for rl in rules:
                counts[rl] += 1
            rows_out.append(
                {
                    "seq": seq_dir.name,
                    "level": level,
                    "verdict": res["verdict"],
                    "blocking": res["blocking_count"],
                    "warnings": res["warning_count"],
                    "rules": ";".join(rules),
                }
            )
    with open(RES_DIR / "diffguard_e5_eval.csv", "w", newline="") as f:
        w = csv.DictWriter(f, list(rows_out[0].keys()))
        w.writeheader()
        w.writerows(rows_out)
    return dict(counts)


def eval_diffguard_corpora() -> dict:
    """Scan E1 (n=100), E6 (n=33), and the 40 screened human apps (20 pre, 20 post) for slop-scan blind spots."""
    e1_csv = STUDY / "e1-corpus/results/runs.csv"
    e6_csv = STUDY / "e6-oneshot/results/runs.csv"
    hum_csv = STUDY / "e1-corpus/results/human_corpus.csv"
    e1_root = STUDY / "e1-corpus/runs"
    e6_root = WORK / "e6/runs"
    hum_root = WORK / "human-corpus"

    groups = defaultdict(list)
    with open(e1_csv) as f:
        for r in csv.DictReader(f):
            if str(r.get("pilot", "")).lower() in ("1", "true", "yes"):
                continue
            if str(r.get("valid", "")).lower() not in ("1", "true", "yes"):
                continue
            grp = "E1-guardrailed" if r["prompt_style"] == "guardrailed" else "E1-unguardrailed"
            groups[grp].append((r["run_id"], e1_root / r["run_id"] / "src"))

    with open(e6_csv) as f:
        for r in csv.DictReader(f):
            if str(r.get("valid", "")).lower() not in ("1", "true", "yes"):
                continue
            groups[f"E6-{r['arm']}"].append((r["run_id"], e6_root / r["run_id"] / "src"))

    # Map human_corpus.csv repo -> directory in hum_root
    hum_dirs = {d.name.lower(): d for d in hum_root.iterdir() if d.is_dir()} if hum_root.exists() else {}
    with open(hum_csv) as f:
        for r in csv.DictReader(f):
            repo = r["repo"]
            slug1 = repo.replace("/", "__").lower()
            slug2 = repo.replace("/", "_").lower()
            slug3 = repo.split("/")[-1].lower()
            d = hum_dirs.get(slug1) or hum_dirs.get(slug2) or hum_dirs.get(slug3)
            if d is None:
                # Fallback substring search
                for k, v in hum_dirs.items():
                    if slug3 in k:
                        d = v
                        break
            if d is not None:
                groups[f"human-{r['stratum']}"].append((repo, d))

    rows_out = []
    summary = {}
    for grp, items in sorted(groups.items()):
        c = Counter()
        c["n"] = len(items)
        for rid, src in items:
            res = diffguard.scan_project(src)
            c[f"verdict_{res['verdict']}"] += 1
            for rl in res["rule_ids"]:
                c[rl] += 1
            rows_out.append(
                {
                    "group": grp,
                    "id": rid,
                    "verdict": res["verdict"],
                    "blocking": res["blocking_count"],
                    "warnings": res["warning_count"],
                    "rules": ";".join(res["rule_ids"]),
                }
            )
        summary[grp] = dict(c)

    with open(RES_DIR / "diffguard_corpus.csv", "w", newline="") as f:
        w = csv.DictWriter(f, list(rows_out[0].keys()))
        w.writeheader()
        w.writerows(rows_out)
    return summary


def main() -> int:
    out = {
        "scale_probe": eval_scale_probe(),
        "diffguard_e4": eval_diffguard_e4(),
        "diffguard_e5": eval_diffguard_e5(),
        "diffguard_corpora": eval_diffguard_corpora(),
    }
    (RES_DIR / "eval_summary.json").write_text(json.dumps(out, indent=2) + "\n")
    print(json.dumps(out, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
