#!/usr/bin/env python3
"""Generate results/runs.csv (contract 5 columns first, then R7/R11 extras) from runs/*/meta.json."""
import csv, json, pathlib

E1 = pathlib.Path(__file__).resolve().parents[1]
COLS = ["run_id", "tool", "model", "model_version", "prompt_style", "rep", "date", "duration_s", "tokens_in", "tokens_out", "cost_usd",
        "files", "kotlin_loc", "build", "build_error_first_line",
        "brief", "bundle", "pilot", "valid", "exit_code", "prompt_sha256", "cli_flags", "cli_version", "harness_config_hash",
        "guardrail_sha256", "transcript_path", "temperature_or_seed", "build_zero_touch", "build_repaired", "net_audit", "n_commands", "denied_actions", "invalid_reason", "web_tool_calls", "web_tools", "web_denied", "generated_at", "outside_refs"]

def row(m, b):
    r = dict(m)
    r["model"] = m.get("model_arg")
    r["model_version"] = m.get("model_reported") or m.get("cli_version")
    r["build"] = b.get("build", "skip"); r["build_error_first_line"] = b.get("build_error_first_line", "")
    r["build_zero_touch"] = b.get("build_zero_touch", ""); r["build_repaired"] = b.get("build_repaired", "")
    return {c: r.get(c, "") for c in COLS}

def main():
    oa = E1 / "results" / "outside_access.csv"                      # harness/audit_outside.py: read/write references outside the run's own directory
    outside = {r["run_id"]: r["outside_refs"] for r in csv.DictReader(open(oa))} if oa.exists() else {}
    rows = []
    for d in sorted((E1 / "runs").glob("*/meta.json")):
        m = json.loads(d.read_text())
        bf = d.parent / "build.json"
        rows.append(row({**m, "outside_refs": outside.get(m.get("run_id"), "")}, json.loads(bf.read_text()) if bf.exists() else {}))
    with open(E1 / "results" / "runs.csv", "w", newline="") as f:
        w = csv.DictWriter(f, COLS); w.writeheader(); w.writerows(rows)
    print(len(rows), "runs ->", E1 / "results" / "runs.csv")

if __name__ == "__main__":
    main()
