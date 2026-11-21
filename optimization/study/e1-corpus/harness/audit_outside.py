#!/usr/bin/env python3
"""Containment audit: did a generator look outside its own work directory? (protocol 1: no access to the study, the detector, other briefs, other runs)

  python3 harness/audit_outside.py            # all runs/*, writes results/outside_access.csv

The Landlock jail restricts WRITES only, reads are open, so containment of reads is audited from the transcripts: every absolute path in a tool call
(shell command, file path argument, glob) that points into the work volume or the home directory but not into the run's own directory counts.
Paths into the toolchain (/usr, /bin, /etc, /proc, /dev, /opt, /lib, /sys, JDK, /tmp) are ignored.
"""
import csv, gzip, json, pathlib, re, sys

E1 = pathlib.Path(__file__).resolve().parents[1]
WORK = "/mnt/01DC198BE9192500/github/Schooldev/3S-study-work"
KEYS = {"command", "cmd", "commandLine", "command_line", "CommandLine", "shell_command", "Path", "path", "file_path", "AbsolutePath", "SearchPath", "DirectoryPath",
        "Directory", "TargetFile", "pattern", "Pattern", "cwd", "directory", "dir", "paths", "File", "TargetDirectory", "SearchDirectory"}
ABS = re.compile(r"(?<![\w.\-])(/(?:mnt|home|root|media|srv|var/lib)/[^\s'\"\\;|&)<>]*)")


def tool_strings(o, out, under=False):
    if isinstance(o, dict):
        for k, v in o.items():
            if k in KEYS:
                if isinstance(v, str): out.append(v)
                elif isinstance(v, list): out.extend(str(x) for x in v)
            else:
                tool_strings(v, out)
    elif isinstance(o, list):
        for v in o: tool_strings(v, out)


def audit(rid, trans):
    own = f"{WORK}/corpus/{rid}"
    refs = []
    for line in trans.splitlines():
        try: o = json.loads(line)
        except ValueError: continue
        if not isinstance(o, dict): continue
        if o.get("event") == "init" or o.get("type") == "system": continue            # init events list cwd, plugins, tools: not an access
        out = []
        tool_strings(o, out)
        for s in out:
            for m in ABS.finditer(s):
                p = m.group(1).rstrip("/.,:")
                if p.startswith(own) or p.startswith(WORK + "/corpus-raw/" + rid) or p.startswith(WORK + "/tmp") or p.startswith(WORK + "/run-homes") or p.startswith(WORK + "/xdg-cache"): continue
                if p == "/mnt" or p == "/home": continue
                refs.append(p)
    return sorted(set(refs))


def main():
    rows = []
    for d in sorted((E1 / "runs").glob("*")):
        p = d / "transcript_full.jsonl.gz"
        if not p.exists(): p = d / "transcript.jsonl.gz"
        if not p.exists(): continue
        refs = audit(d.name, gzip.open(p, "rt", errors="replace").read())
        rows.append({"run_id": d.name, "outside_refs": len(refs), "paths": " | ".join(refs)[:600]})
    with open(E1 / "results" / "outside_access.csv", "w", newline="") as f:
        w = csv.DictWriter(f, ["run_id", "outside_refs", "paths"]); w.writeheader(); w.writerows(rows)
    bad = [r for r in rows if r["outside_refs"]]
    print(f"{len(bad)} of {len(rows)} runs reference paths outside their work directory")
    for r in bad: print(" ", r["run_id"], r["outside_refs"], r["paths"][:200])


if __name__ == "__main__":
    main()
