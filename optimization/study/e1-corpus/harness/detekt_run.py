#!/usr/bin/env python3
"""Run detekt 2.0.0-alpha.6 + compose-rules 0.6.8 (supported pair, compose-rules docs/detekt.md) on a source tree; keep only the Compose rule set.

  detekt_run.py <src_dir> <run_id> -> prints findings as CSV rows (detector=detekt) to stdout; source-level, needs no build.
"""
import csv, os, pathlib, subprocess, sys, xml.etree.ElementTree as ET

TOOLS = pathlib.Path("/mnt/01DC198BE9192500/github/Schooldev/3S-study-work/tools/detekt")
TMP = pathlib.Path("/mnt/01DC198BE9192500/github/Schooldev/3S-study-work/tmp/beta-f2-2")

def compose_rule_names():
    import re, zipfile
    cfg = zipfile.ZipFile(TOOLS / "compose-rules-detekt-0.6.8.jar").read("config/config.yml").decode()
    return set(re.findall(r"^  (\w+):\s*$", cfg, re.M))

def run(src, run_id):
    out = TMP / f"detekt-{run_id}.xml"; out.parent.mkdir(parents=True, exist_ok=True)
    p = subprocess.run(["java", "-Xmx1g", "-jar", str(TOOLS / "detekt-cli-2.0.0-alpha.6-all.jar"), "--input", str(src), "--plugins", ":".join(str(x) for x in sorted(TOOLS.glob("compose-rules-*-0.6.8.jar"))),
                        "--build-upon-default-config", "--report", f"checkstyle:{out}", "--base-path", str(src)], capture_output=True, text=True, timeout=900, env={**os.environ, "TMPDIR": str(TMP)})
    rows, names = [], compose_rule_names()
    if out.exists():
        for f in ET.parse(out).getroot().iter("file"):
            for e in f.iter("error"):
                src_id = e.get("source", "")
                if src_id.split(".")[-1] in names:
                    rows.append({"run_id": run_id, "rule_id": "detekt:" + src_id.split(".")[-1], "file": f.get("name"), "line": e.get("line"), "detector": "detekt", "evidence": e.get("message", "")[:160], "triage": "unk"})
        out.unlink()
    return rows, p.returncode, (p.stderr + p.stdout)[-300:] if p.returncode not in (0, 2) else ""

if __name__ == "__main__":
    rows, rc, err = run(sys.argv[1], sys.argv[2])
    w = csv.DictWriter(sys.stdout, ["run_id", "rule_id", "file", "line", "detector", "evidence", "triage"]); w.writeheader(); w.writerows(rows)
    print(f"# rc={rc} rows={len(rows)} {err}", file=sys.stderr)
