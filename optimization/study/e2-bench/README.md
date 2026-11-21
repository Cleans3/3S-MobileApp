# e2-bench

Study E2: what each slop pattern costs (paired A/B screens in the optlab app). Start with `REPORT.md`.

| path | what |
|---|---|
| `REPORT.md` | method, environment, results tables, threats, open items |
| `results/raw.csv` | per-iteration values: factor_id,variant,metric,unit,iteration,value,device,build_type,commit |
| `results/summary.csv` | medians, IQR, delta %, bootstrap CI, Cliff's delta, p, n per factor and metric (`summary.md` is the same as a table) |
| `results/runlog.csv` | one row per am-instrument call: seconds, result, host memory and swap at that moment |
| `results/environment.json` | device, toolchain versions, APK sha256, commit |
| `results/build_facts.csv` | one-off facts quoted in the report (profile sizes, DB sizes, FTS5 absent, heap limit) |
| `results/p01p09-n20000-A-oom.txt` | logcat evidence for the A variant failing at 20,000 rows |
| `variants.csv`, `GROUND_TRUTH.md` | provenance of each A/B variant; seeded defects for E5 |
| `harness/run_bench.py` | ABAB harness (resumable); `summarize.py` raw to summary; `record_demo.py` demo clips |

Sources: `android/optlab` (lab app), `android/benchmark` (Macrobenchmark module). Heavy artifacts live outside the repo in `3S-study-work/` (`apk/`, `video/`, `e2-traces/`).
