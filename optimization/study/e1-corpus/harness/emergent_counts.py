#!/usr/bin/env python3
"""Descriptive counters for patterns the frozen taxonomy does not name (open-coding aid, all valid runs). Feeds no endpoint.

  python3 harness/emergent_counts.py   -> results/open_coding/emergent_counts.csv (one row per valid non-pilot run, 0/1 flags and counts)

Each pattern is a plain regex over generated Kotlin/Gradle/XML/ProGuard text; a hit is a pointer for the human coder, not a verdict.
Brief coverage: the endpoint paths each brief names (protocol section 8) versus the paths found in the generated code.
"""
import csv, pathlib, re

E1 = pathlib.Path(__file__).resolve().parents[1]
RES = E1 / "results"
PATTERNS = {   # name -> (glob suffixes, regex)
    "release_signed_debug": ((".kts", ".gradle"), r"release\s*\{[^}]*signingConfig\s*=\s*signingConfigs\.(getByName\(\"debug\"\)|debug)"),
    "keep_all_rule": ((".pro",), r"^-keep\s+class\s+[\w.*]+\s*\{\s*\*;?\s*\}|^-keep\s+class\s+\*\*"),
    "minify_without_keep": ((".kts", ".gradle"), r"isMinifyEnabled\s*=\s*true"),
    "runcatching_swallow": ((".kt",), r"runCatching\s*\{[^}]*\}\s*\.(getOrNull|getOrDefault|onFailure\s*\{\s*\})|catch\s*\(\s*(e|_|t|ex)\s*:\s*(Exception|Throwable)\s*\)\s*\{\s*\}"),
    "placeholder_base_url": ((".kt", ".kts", ".gradle", ".xml"), r"https?://[\w.\-]*(example\.(com|org)|\.example|localhost|10\.0\.2\.2|yourapi|your-api)"),
    "bang_bang": ((".kt",), r"!!"),
    "literal_ui_strings": ((".kt",), r"\b(Text|Button|TextButton|OutlinedButton)\(\s*(text\s*=\s*)?\"[A-Za-z]"),
    "fake_delay_or_mock": ((".kt",), r"\bdelay\(\s*\d{3,}|\bThread\.sleep\(|fakeData|mockData|sampleBooks|dummy"),
    "viewmodel_factory_boilerplate": ((".kt",), r"ViewModelProvider\.Factory"),
    "manual_di_in_application": ((".kt",), r"class\s+\w+\s*:\s*Application\(\)"),
    "unchecked_cast_suppress": ((".kt",), r"@Suppress\(\"UNCHECKED_CAST\"\)"),
    "security_crypto_deprecated": ((".kt", ".kts", ".toml", ".gradle"), r"security-crypto|EncryptedSharedPreferences|MasterKey"),
    "gson_reflection": ((".kt", ".kts", ".toml", ".gradle"), r"converter-gson|GsonConverterFactory|com\.google\.gson"),
    "nav_compose": ((".kt", ".kts", ".toml", ".gradle"), r"navigation-compose|NavHost\("),
    "hilt_or_koin": ((".kt", ".kts", ".toml", ".gradle"), r"hilt|dagger|koin"),
    "previews": ((".kt",), r"@Preview"),
    "tests_present": ((".kt",), r"androidx\.test|junit|kotlin\.test"),
}
BRIEF_PATHS = {
    "B1": ["api/titles", "api/titles/"],
    "B2": ["auth/login", "members/me", "logout", "password"],
    "B3": ["me/loans", "me/history", "renew", "me/privacy"],
}


def main():
    runs = [r for r in csv.DictReader(open(RES / "runs.csv")) if r["pilot"] != "True" and r["valid"] == "True"]
    out_rows = []
    for r in runs:
        src = E1 / "runs" / r["run_id"] / "src"
        texts = {}
        for p in src.rglob("*"):
            if p.is_file() and p.suffix in (".kt", ".kts", ".gradle", ".xml", ".pro", ".toml"):
                texts.setdefault(p.suffix, []).append(p.read_text(errors="replace"))
        row = {"run_id": r["run_id"], "bundle": r["bundle"], "style": r["prompt_style"], "brief": r["brief"]}
        for name, (suffixes, rx) in PATTERNS.items():
            n = sum(len(re.findall(rx, t, re.M)) for s in suffixes for t in texts.get(s, []))
            row[name] = n
        row["minify_without_keep"] = int(row["minify_without_keep"] > 0 and not any(len(t.strip().splitlines()) > 1 and re.search(r"^-keep", t, re.M) for t in texts.get(".pro", [])))
        alltxt = "\n".join(t for ts in texts.values() for t in ts)
        kt_lines = [ln for t in texts.get(".kt", []) for ln in t.splitlines() if ln.strip()]
        row["max_line_len"] = max((len(ln) for ln in kt_lines), default=0)
        row["lines_gt200"] = sum(1 for ln in kt_lines if len(ln) > 200)
        want = BRIEF_PATHS[r["brief"]]
        row["brief_paths_found"] = sum(1 for p in want if p in alltxt)
        row["brief_paths_total"] = len(want)
        out_rows.append(row)
    d = RES / "open_coding"; d.mkdir(exist_ok=True)
    with open(d / "emergent_counts.csv", "w", newline="") as f:
        w = csv.DictWriter(f, list(out_rows[0])) if out_rows else None
        if w: w.writeheader(); w.writerows(out_rows)
    print(len(out_rows), "runs ->", d / "emergent_counts.csv")


if __name__ == "__main__":
    main()
