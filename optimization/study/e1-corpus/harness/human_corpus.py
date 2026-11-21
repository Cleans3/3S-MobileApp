#!/usr/bin/env python3
"""Draw the human control corpus (protocol 7, R3): >= 30 public Android Compose apps in two strata.

  pre-assistant: first commit <= 2022-11-30, pinned at the last default-branch commit <= 2022-11-30 (confirmatory comparator C3)
  2024+        : first commit >= 2024-01-01, pinned at default-branch head on the draw date (second comparator, not confirmatory)
Candidates come from GitHub search (queries + date recorded), are shuffled with seed 20261121 and screened IN THAT ORDER against the
inclusion criteria (Kotlin, androidx.compose, application module, 2,000-50,000 Kotlin LOC, not a vendor sample/codelab/tutorial).
Writes results/human_corpus.csv (accepted), results/human_corpus_screen.csv (every screened repo + reason), results/human_corpus_queries.json.
"""
import concurrent.futures as cf, csv, datetime, json, os, pathlib, random, re, shutil, subprocess, sys, time

HERE = pathlib.Path(__file__).resolve().parent
E1 = HERE.parent
sys.path.insert(0, str(E1 / "slop-scan"))
WORK = pathlib.Path("/mnt/01DC198BE9192500/github/Schooldev/3S-study-work/human-corpus")
SEED, TARGET = 20261121, 20
CUT = "2022-12-01T00:00:00Z"
STRATA = {
    "pre": {"created": "2021-06-01..2022-11-30"},
    "post": {"created": ">=2024-01-01"},
}
BANDS = ["5..30", "31..100", "101..400", "401..5000"]
QUERIES = [("topic:jetpack-compose", "A"), ("topic:compose", "B"), ('"jetpack compose" in:readme', "C")]
VENDOR = re.compile(r"sample|codelab|tutorial|demo|course|workshop|learn|example|template|boilerplate|playground|study|exercise|bootcamp|cheat|awesome|compose-samples|showcase", re.I)
VENDOR_OWNER = {"android", "googlesamples", "googlecodelabs", "google", "jetbrains", "microsoft", "square", "airbnb"}
# GIT_CONFIG_*: /mnt is a root-owned fuseblk mount, git refuses its repositories as 'dubious ownership' otherwise
ENV = {**os.environ, "GIT_TERMINAL_PROMPT": "0", "GIT_ASKPASS": "true", "GIT_CONFIG_COUNT": "1", "GIT_CONFIG_KEY_0": "safe.directory", "GIT_CONFIG_VALUE_0": "*", "TMPDIR": "/mnt/01DC198BE9192500/github/Schooldev/3S-study-work/tmp/beta-f2-2"}

def run(*a, cwd=None, timeout=600):
    return subprocess.run(a, cwd=cwd, env=ENV, capture_output=True, text=True, timeout=timeout)

def search(stratum, log):
    pool = {}
    for qtext, qid in QUERIES:
        for band in BANDS:
            q = f"language:Kotlin {qtext} created:{STRATA[stratum]['created']} stars:{band}"
            for page in (1, 2, 3):
                r = run("gh", "api", "-X", "GET", "search/repositories", "-f", f"q={q}", "-f", "per_page=100", "-f", f"page={page}", "-f", "sort=stars")
                if r.returncode != 0: log.append({"q": q, "page": page, "error": r.stderr[:120]}); time.sleep(20); break
                items = json.loads(r.stdout).get("items", [])
                for it in items:
                    d = pool.setdefault(it["full_name"], {"stars": it["stargazers_count"], "created": it["created_at"][:10], "desc": it.get("description") or "", "archived": it["archived"], "fork": it["fork"], "qids": set()})
                    d["qids"].add(qid)
                log.append({"q": q, "page": page, "n": len(items)})
                time.sleep(2.2)
                if len(items) < 100: break
    return pool

def kt_stats(d):
    import slopscan
    p = slopscan.Project(d)
    gt = p.gradletext + "\n".join(t for _, t in p.gradle)
    app = any(re.search(r"com\.android\.application|android\.application", t) for _, t in p.gradle)
    compose = bool(re.search(r"androidx\.compose|compose\s*=\s*true|kotlin\.compose|composeOptions|compose-bom|compose\.bom", gt))
    return p.kt_loc, app, compose

def screen(repo, info, stratum):
    owner, name = repo.split("/")
    if info["fork"] or info["archived"]: return {"repo": repo, "ok": False, "reason": "fork/archived"}
    if owner.lower() in VENDOR_OWNER or VENDOR.search(name) or VENDOR.search(info["desc"]): return {"repo": repo, "ok": False, "reason": "vendor/sample/tutorial by name/description/owner"}
    d = WORK / repo.replace("/", "__")
    try:
        if not (d / ".git").exists():
            shutil.rmtree(d, ignore_errors=True)
            r = run("git", "clone", "--filter=blob:none", "--no-checkout", "--quiet", f"https://github.com/{repo}.git", str(d), timeout=300)
            if r.returncode: return {"repo": repo, "ok": False, "reason": "clone failed: " + r.stderr.strip()[:80]}
        root = run("git", "rev-list", "--max-parents=0", "HEAD", cwd=d).stdout.split()
        first = min(run("git", "show", "-s", "--format=%cI", c, cwd=d).stdout.strip() for c in root) if root else ""
        if stratum == "pre":
            if not first or first[:10] > "2022-11-30": return {"repo": repo, "ok": False, "reason": f"first commit {first[:10]} after cutoff"}
            pin = run("git", "rev-list", "-1", "--first-parent", f"--before={CUT}", "HEAD", cwd=d).stdout.strip()
            if not pin: return {"repo": repo, "ok": False, "reason": "no default-branch commit before cutoff"}
        else:
            if not first or first[:10] < "2024-01-01": return {"repo": repo, "ok": False, "reason": f"first commit {first[:10]} before 2024"}
            pin = run("git", "rev-parse", "HEAD", cwd=d).stdout.strip()
        r = run("git", "checkout", "--quiet", "--detach", pin, cwd=d, timeout=600)
        if r.returncode: return {"repo": repo, "ok": False, "reason": "checkout failed: " + r.stderr.strip()[:80]}
        loc, app, compose = kt_stats(d)
        if not app: return {"repo": repo, "ok": False, "reason": "no application module"}
        if not compose: return {"repo": repo, "ok": False, "reason": "no androidx.compose in Gradle files"}
        if not 2000 <= loc <= 50000: return {"repo": repo, "ok": False, "reason": f"kotlin_loc {loc} outside 2000-50000"}
        shutil.rmtree(d / ".git", ignore_errors=True)       # sources only; commit sha is recorded in the CSV
        return {"repo": repo, "ok": True, "reason": "", "commit": pin, "first": first[:10], "loc": loc}
    except subprocess.TimeoutExpired:
        return {"repo": repo, "ok": False, "reason": "git timeout"}
    finally:
        pass

def main():
    WORK.mkdir(parents=True, exist_ok=True)
    qlog, accepted, screened = [], [], []
    meta = {"date": datetime.date.today().isoformat(), "seed": SEED, "target_per_stratum": TARGET, "queries": [f"language:Kotlin {q} created:<range> stars:<band>" for q, _ in QUERIES], "bands": BANDS, "strata": STRATA, "pool": {}}
    for stratum in STRATA:
        pool = search(stratum, qlog)
        names = sorted(pool); random.Random(SEED).shuffle(names)
        meta["pool"][stratum] = len(names)
        got, rank, i = 0, 0, 0
        print(stratum, "pool", len(names), flush=True)
        with cf.ThreadPoolExecutor(4) as ex:
            while got < TARGET and i < len(names):
                win = names[i:i + 8]; i += 8
                res = list(ex.map(lambda n: screen(n, pool[n], stratum), win))
                for n, r in zip(win, res):                       # accept strictly in draw order
                    rank += 1
                    screened.append({"repo": n, "stratum": stratum, "rank": rank, "accepted": int(r["ok"] and got < TARGET), "reason": r["reason"]})
                    if r["ok"] and got < TARGET:
                        got += 1
                        accepted.append({"repo": n, "commit": r["commit"], "creation_year": pool[n]["created"][:4], "stratum": stratum, "kotlin_loc": r["loc"],
                                         "first_commit": r["first"], "stars": pool[n]["stars"], "query_ids": "".join(sorted(pool[n]["qids"])), "draw_rank": rank})
                    elif r["ok"]:
                        shutil.rmtree(WORK / n.replace("/", "__"), ignore_errors=True)
                print(stratum, "accepted", got, "of", rank, "screened", flush=True)
    res = E1 / "results"
    with open(res / "human_corpus.csv", "w", newline="") as f:
        w = csv.DictWriter(f, ["repo", "commit", "creation_year", "stratum", "kotlin_loc", "first_commit", "stars", "query_ids", "draw_rank"]); w.writeheader(); w.writerows(accepted)
    with open(res / "human_corpus_screen.csv", "w", newline="") as f:
        w = csv.DictWriter(f, ["repo", "stratum", "rank", "accepted", "reason"]); w.writeheader(); w.writerows(screened)
    meta["search_log_entries"] = len(qlog); meta["search_errors"] = [x for x in qlog if "error" in x]
    (res / "human_corpus_queries.json").write_text(json.dumps(meta, indent=1))
    print("done", len(accepted))

if __name__ == "__main__":
    main()
