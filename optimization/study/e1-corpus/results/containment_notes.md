# Containment audit (audit_outside.py, results/outside_access.csv)

4 of 107 runs (incl. 6 pilots) name a path outside their own work directory. Triage of each by reading the transcript:

| run | what | outcome |
|---|---|---|
| agy-g38flash-B1-naive-r2 | listed and read a sibling run directory (read-only, jail covers writes only) | run is the harness-limited invalid run; not in any estimate |
| claude-haiku-B3-naive-r1 | Write to 3S-study-work/loans/src/... | permission_denied by the tool (outside allowed working directories); the model rewrote the file inside its own directory; nothing exists at the outside path |
| codex-luna-B2-naive-r1 | file_change add of 3S-study-work/.gitignore | status failed (Landlock write-jail); the file does not exist |
| codex-luna-B3-naive-r3 | ls of /opt/android-sdk/platforms and /root/.gradle/caches/... (toolchain probe, 2>/dev/null) | read of absent paths; no content seen |

Result: no write escaped a work directory. One run read another run's directory (invalid run). Reads outside the directory are not blocked by design; the audit scans for them.
