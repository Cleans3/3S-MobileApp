# e2-bench

Study E2: what each slop pattern costs (paired A/B screens in the optlab app). Start with `REPORT.md`.

| path | what |
|---|---|
| `REPORT.md` | method, environment, results tables, threats, open items |
| `results/raw.csv` | per-iteration values of kept iterations: factor_id,variant,metric,unit,iteration,value,device,build_type,commit,mem_avail_mb,loadavg1 |
| `results/summary.csv` | medians, IQR, delta %, bootstrap CI, Cliff's delta, p, n per factor and metric (`summary.md` is the same as a table) |
| `results/runlog.csv` | one row per am-instrument call: phase, seconds, result, host memory and swap, iterations kept and dropped, lock wait |
| `results/iterlog.csv`, `results/exclusions.csv` | every iteration with host memory, load, swap-in pages and kept/dropped status; per factor, phase and variant counts of the pre-declared exclusion rule |
| `results/p13_equivalence.csv` | P13: result counts and Jaccard of A and B result sets for every typed prefix |
| `results/pilot-run1/` | the first full run (before the host rules and corrections), kept as the pilot reference (protocol §15 A3) |
| `results/environment.json` | device, toolchain versions, APK sha256, commit |
| `results/build_facts.csv` | one-off facts quoted in the report (profile sizes, DB sizes, FTS5 absent, heap limit) |
| `results/p01p09-n20000-A-oom.txt` | logcat evidence for the A variant failing at 20,000 rows |
| `ports/` | corpus code as the A variant: `README.md` (rule, one row per factor), `<factor>.diff` (corpus file against the port), `rejections.csv`, `make_ports.py` |
| `variants.csv`, `GROUND_TRUTH.md` | provenance of each A/B variant (author-built `P08`...`P03P04` as sensitivity, corpus ports `P08c`...`P03P04c`); seeded defects for E5 |
| `harness/run_bench.py` | ABAB harness (resumable, takes bench.lock per factor group, two phases, `--patient` waits without the turnstile); `summarize.py` raw to summary + exclusions; `report_tables.py` summary to the REPORT tables; `record_demo.py` demo clips |

Sources: `android/optlab` (lab app), `android/benchmark` (Macrobenchmark module). Heavy artifacts live outside the repo in `3S-study-work/` (`apk/`, `video/`; Perfetto traces are not kept, set `TRACE_DIR` to keep round-0 traces).
