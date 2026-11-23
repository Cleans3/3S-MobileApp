#!/usr/bin/env python3
"""results/summary.csv (+ exclusions.csv, p13_equivalence.csv) -> markdown tables for REPORT.md section 4, 6 and the exclusion table.
Prints one block per table, headed by '@@<name>'; the report text around them is written by hand. Standard library only.
Usage: python3 report_tables.py [results_dir]
"""
import csv, sys
from pathlib import Path

RES = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parent.parent / "results"
SHOW = {
    "P08": ["st.fx.perItem", "st.rc.row", "Compose_recompose.count", "Recomposer_recompose.mainMs", "Compose_recompose.mainMs",
            "frameDurationCpuMs_p90", "frameDurationCpuMs_p99", "frameOverrunMs_p90", "slowFrames_pct", "memoryHeapSizeMaxKb", "memoryRssAnonMaxKb"],
    "P06": ["st.decodedBytes", "st.decodes", "memoryRssAnonMaxKb", "memoryHeapSizeMaxKb", "st_img_decode.sumMs", "st_img_decode.maxMs",
            "frameDurationCpuMs_p90", "frameDurationCpuMs_p99", "frameOverrunMs_p90", "slowFrames_pct"],
    "P01+P09": ["st.rc.row", "Compose_recompose.count", "Compose_recompose.mainMs", "timeToInitialDisplayMs", "timeToFullDisplayMs",
                "memoryHeapSizeMaxKb", "memoryRssAnonMaxKb", "frameDurationCpuMs_p90", "frameDurationCpuMs_p99", "frameOverrunMs_p90", "slowFrames_pct"],
    "P01+P09/P01": ["st.rc.row", "Compose_recompose.count", "Compose_recompose.mainMs", "Recomposer_recompose.mainMs", "timeToInitialDisplayMs", "timeToFullDisplayMs", "memoryHeapSizeMaxKb", "memoryRssAnonMaxKb"],
    "P13": ["st.search.queries", "st_search_query.count", "st_search_query.mainMs", "st_search_query.sumMs", "st_search_query.maxMs",
            "memoryHeapSizeMaxKb", "memoryRssAnonMaxKb", "frameDurationCpuMs_p90", "frameDurationCpuMs_p99", "frameOverrunMs_p90", "slowFrames_pct"],
    "P03+P04": ["st.rc.row", "st_sort.count", "st_sort.mainMs", "Compose_recompose.count", "Recomposer_recompose.mainMs",
                "Compose_recompose.mainMs", "frameDurationCpuMs_p90", "frameDurationCpuMs_p99", "frameOverrunMs_p90", "slowFrames_pct"],
    "P10": ["apk_bytes", "dex_bytes", "resources_bytes", "timeToInitialDisplayMs", "timeToFullDisplayMs", "frameDurationCpuMs_p90",
            "frameOverrunMs_p90", "slowFrames_pct"],
    "P11": ["timeToInitialDisplayMs", "timeToFullDisplayMs", "frameDurationCpuMs_p90", "frameDurationCpuMs_p99", "frameOverrunMs_p90", "slowFrames_pct"],
}


def g(x):
    return "—" if x == "" else x


def main():
    rows = {(r["factor_id"], r["metric"]): r for r in csv.DictReader((RES / "summary.csv").open())}
    for fid, metrics in SHOW.items():
        print(f"@@{fid}")
        print("| metric | unit | A median (IQR) | B median (IQR) | Δ% (B vs A) | 95% CI of Δ% | Δ abs (B − A) | 95% CI of Δ abs | Cliff's δ | n |")
        print("|---|---|---|---|---|---|---|---|---|---|")
        for m in metrics:
            r = rows.get((fid, m))
            if not r:
                continue
            ci = f"[{r['delta_ci95_lo']}, {r['delta_ci95_hi']}]" if r["delta_ci95_lo"] != "" else "—"
            aci = f"[{r['delta_abs_ci95_lo']}, {r['delta_abs_ci95_hi']}]" if r["delta_abs_ci95_lo"] != "" else "—"
            print(f"| `{m}` | {r['unit']} | {r['A_median']} ({r['A_iqr']}) | {r['B_median']} ({r['B_iqr']}) | {g(r['delta_pct'])} | {ci} | {g(r['delta_abs'])} | {aci} | {g(r['cliffs_delta'])} | {r['n']} |")
        print()
    ex = RES / "exclusions.csv"
    if ex.exists():
        print("@@exclusions")
        print("| factor | phase | variant | iterations run | kept | dropped | of which mem < 1024 MB | of which swap-in | no sample |")
        print("|---|---|---|---|---|---|---|---|---|")
        for r in csv.DictReader(ex.open()):
            print(f"| {r['factor_id']} | {r['phase']} | {r['variant']} | {r['iterations_run']} | {r['kept']} | {r['dropped_total']} | {r['dropped_mem_lt_1024']} | {r['dropped_swapin']} | {r['dropped_no_sample']} |")
        print()
    eq = RES / "p13_equivalence.csv"
    if eq.exists():
        print("@@equivalence")
        print("| typed prefix | A (corpus LIKE) results | H (naive token-prefix) results | B (FTS4) uncapped | B page (120, title order) | Jaccard A vs B (all) | Jaccard A page vs B page (120 each, title order) | Jaccard H vs B (all) | Jaccard H page vs B page |")
        print("|---|---|---|---|---|---|---|---|---|")
        for r in csv.DictReader(eq.open()):
            print(f"| `{r['query']}` | {r['a_corpus_like_count']} | {r['h_token_prefix_count']} | {r['b_fts_count_uncapped']} | {r['b_fts_count_page120']} | {r['jaccard_a_vs_b_all']} | {r['jaccard_a_page120_vs_b_page120']} | {r['jaccard_h_vs_b_all']} | {r['jaccard_h_page120_vs_b_page120']} |")
        print()


if __name__ == "__main__":
    main()
