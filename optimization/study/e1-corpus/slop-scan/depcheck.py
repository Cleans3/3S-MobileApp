#!/usr/bin/env python3
"""depcheck: resolve every Gradle coordinate of a project against its declared repositories (S11, D02, H4).

  depcheck.py scan <project_dir> --id <run_id> [--out deps.json] [--snapshot-dir results/maven-snapshot]

For each coordinate (catalog libraries/plugins, dependency strings, plugin ids, classpath):
  class hallucinated-artifact : group:artifact has no maven-metadata.xml in ANY declared repository (404 everywhere)  -> S11
  class hallucinated-version  : artifact exists, declared exact version absent from the metadata                      -> S11
  class vulnerable            : OSV lists a HIGH/CRITICAL advisory for group:artifact@version                          -> S11
  D02                         : >= 1 major behind the latest stable, or the declared release is > 12 months older than the latest stable
Existence only: nothing is published or registered. maven-metadata.xml is snapshotted (generation-date evidence, R7).
JitPack coordinates cannot be proven absent (it builds on demand): class unverifiable-jitpack, not a finding.
"""
import argparse, concurrent.futures as cf, datetime, email.utils, hashlib, json, pathlib, re, sys, threading, time, urllib.error, urllib.request
import xml.etree.ElementTree as ET

HERE = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
REPOS = {"google": "https://dl.google.com/dl/android/maven2", "mavenCentral": "https://repo1.maven.org/maven2",
         "gradlePluginPortal": "https://plugins.gradle.org/m2", "jitpack": "https://jitpack.io"}
UA = {"User-Agent": "study-depcheck/1.0"}
_cache, _lock = {}, threading.Lock()
STABLE = re.compile(r"^\d+(\.\d+)*$|^\d+(\.\d+)*[-.]?(final|release|ga)$", re.I)

def http(url, method="GET", data=None, timeout=25):
    key = (method, url, data)
    with _lock:
        if key in _cache: return _cache[key]
    res = (0, b"", {})
    for attempt in range(3):
        try:
            req = urllib.request.Request(url, data=data, method=method, headers={**UA, **({"Content-Type": "application/json"} if data else {})})
            with urllib.request.urlopen(req, timeout=timeout) as r: res = (r.status, r.read() if method != "HEAD" else b"", dict(r.headers)); break
        except urllib.error.HTTPError as e:
            if e.code in (404, 410): res = (e.code, b"", {}); break
            res = (e.code, b"", {}); time.sleep(1 + attempt)
        except Exception:
            res = (0, b"", {}); time.sleep(1 + attempt)
    with _lock: _cache[key] = res
    return res

def vkey(v):
    return [(0, int(x)) if x.isdigit() else (-1, x) for x in re.split(r"[.\-+_]", v) if x != ""]

def is_stable(v): return bool(STABLE.match(v))

def metadata(repo_url, group, artifact):
    return http(f"{repo_url}/{group.replace('.', '/')}/{artifact}/maven-metadata.xml")

def parse_meta(blob):
    try: root = ET.fromstring(blob)
    except ET.ParseError: return [], None
    vs = [v.text for v in root.iter("version") if v.text]
    return vs, (root.findtext("versioning/release") or root.findtext("versioning/latest"))

# ---------------------------------------------------------------- extraction
DEP_CFG = r"(?:implementation|api|compileOnly|runtimeOnly|kapt|ksp|annotationProcessor|classpath|testImplementation|androidTestImplementation|debugImplementation|releaseImplementation|baselineProfile|lintChecks|coreLibraryDesugaring)"

def toml_sections(t):
    sec, out = None, {}
    for ln in t.splitlines():
        m = re.match(r"\s*\[(\w[\w.\-]*)\]", ln)
        if m: sec = m.group(1); out[sec] = []; continue
        if sec and ln.strip() and not ln.strip().startswith("#"): out[sec].append(ln)
    return out

def extract(proj_root):
    root = pathlib.Path(proj_root)
    coords, repos, vars_ = [], set(), {}
    files = [p for p in root.rglob("*") if p.is_file() and not set(p.relative_to(root).parts) & {"build", ".gradle", ".git", "node_modules"} and
             (p.name in ("build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts", "libs.versions.toml") or (p.suffix == ".toml" and "gradle" in p.parts))]
    texts = {p.relative_to(root).as_posix(): p.read_text(errors="replace") for p in files}
    for path, t in texts.items():
        for m in re.finditer(r"\b(?:val|var|def)\s+(\w+)\s*(?::\s*String)?\s*=\s*[\"']([^\"']+)[\"']|\bext(?:ra)?(?:\.|\[\")(\w+)(?:\"\])?\s*=\s*[\"']([^\"']+)[\"']", t):
            vars_[m.group(1) or m.group(3)] = m.group(2) or m.group(4)
    versions = {}
    for path, t in texts.items():
        if path.endswith(".toml"):
            sec = toml_sections(t)
            for ln in sec.get("versions", []):
                m = re.match(r"\s*([\w\-.]+)\s*=\s*\"([^\"]+)\"", ln)
                if m: versions[m.group(1)] = m.group(2)
                m = re.match(r"\s*([\w\-.]+)\s*=\s*\{[^}]*(?:strictly|require|prefer)\s*=\s*\"([^\"]+)\"", ln)
                if m: versions[m.group(1)] = m.group(2).strip("[]()")
    def ver(spec):
        m = re.search(r"version\.ref\s*=\s*\"([^\"]+)\"", spec)
        if m: return versions.get(m.group(1))
        m = re.search(r"\bversion\s*=\s*\"([^\"]+)\"", spec)
        return m.group(1) if m else None
    for path, t in texts.items():
        line = lambda i: t[:i].count("\n") + 1
        if path.endswith(".toml"):
            sec = toml_sections(t); off = 0
            for ln in sec.get("libraries", []):
                i = t.find(ln)
                m = re.search(r"module\s*=\s*\"([^\":]+):([^\":]+)\"", ln) or re.search(r"group\s*=\s*\"([^\"]+)\"\s*,\s*name\s*=\s*\"([^\"]+)\"", ln)
                if m: coords.append({"group": m.group(1), "artifact": m.group(2), "version": ver(ln), "kind": "lib", "file": path, "line": line(i)})
                else:
                    m = re.match(r"\s*[\w\-.]+\s*=\s*\"([^\":]+):([^\":]+):([^\"]+)\"", ln)
                    if m: coords.append({"group": m.group(1), "artifact": m.group(2), "version": m.group(3), "kind": "lib", "file": path, "line": line(i)})
            for ln in sec.get("plugins", []):
                m = re.search(r"id\s*=\s*\"([^\"]+)\"", ln); i = t.find(ln)
                if m: coords.append({"group": m.group(1), "artifact": m.group(1) + ".gradle.plugin", "version": ver(ln), "kind": "plugin", "file": path, "line": line(i)})
            continue
        for m in re.finditer(r"\bgoogle\(\)|mavenCentral\(\)|gradlePluginPortal\(\)|jcenter\(\)|mavenLocal\(\)|maven\s*\{[^}]*?(?:url\s*=?\s*(?:uri\()?\s*[\"']([^\"']+)[\"'])|maven\s*\(\s*[\"']([^\"']+)[\"']", t):
            s = m.group(0)
            if s.startswith("google"): repos.add("google")
            elif s.startswith("mavenCentral"): repos.add("mavenCentral")
            elif s.startswith("gradlePluginPortal"): repos.add("gradlePluginPortal")
            elif s.startswith("jcenter"): repos.add("jcenter")
            elif m.group(1) or m.group(2): repos.add((m.group(1) or m.group(2)).rstrip("/"))
        for m in re.finditer(DEP_CFG + r"\s*\(?\s*(?:platform\s*\(\s*|enforcedPlatform\s*\(\s*)?[\"']([\w.\-]+):([\w.\-]+)(?::([^\"']+))?[\"']", t):
            v = m.group(3)
            if v: v = re.sub(r"^\$\{?(\w+)\}?$", lambda x: vars_.get(x.group(1), ""), v) or None
            coords.append({"group": m.group(1), "artifact": m.group(2), "version": v, "kind": "bom" if "platform" in m.group(0) else "lib", "file": path, "line": line(m.start())})
        for m in re.finditer(r"\bid\s*\(?\s*[\"']([\w.\-]+)[\"']\s*\)?\s*(?:version\s*[\"']([^\"']+)[\"'])?", t):
            if m.group(1).startswith("org.gradle"): continue
            coords.append({"group": m.group(1), "artifact": m.group(1) + ".gradle.plugin", "version": m.group(2), "kind": "plugin", "file": path, "line": line(m.start())})
        for m in re.finditer(r"\bkotlin\s*\(\s*[\"'](\w+)[\"']\s*\)\s*version\s*[\"']([^\"']+)[\"']", t):
            coords.append({"group": "org.jetbrains.kotlin." + m.group(1), "artifact": "org.jetbrains.kotlin." + m.group(1) + ".gradle.plugin", "version": m.group(2), "kind": "plugin", "file": path, "line": line(m.start())})
    seen, out = set(), []
    for c in coords:
        k = (c["group"], c["artifact"], c["version"], c["kind"])
        if k in seen: continue
        seen.add(k)
        if c["kind"] == "plugin" and c["group"].startswith(("kotlin", "com.android")) and c["group"] in ("kotlin", "kotlin-android"): continue
        out.append(c)
    return out, repos

# ---------------------------------------------------------------- resolution
def repo_urls(kind, declared):
    urls = []
    for r in sorted(declared):
        if r in REPOS: urls.append((r, REPOS[r]))
        elif r == "jcenter": urls.append(("jcenter", "https://repo1.maven.org/maven2"))
        elif r.startswith("http"): urls.append((r, r))
    base = [("google", REPOS["google"]), ("mavenCentral", REPOS["mavenCentral"])] if kind != "plugin" else [("gradlePluginPortal", REPOS["gradlePluginPortal"]), ("google", REPOS["google"]), ("mavenCentral", REPOS["mavenCentral"])]
    have = {n for n, _ in urls}
    if kind == "plugin":      # plugin resolution defaults (pluginManagement) when the project declares nothing for plugins
        urls += [x for x in base if x[0] not in have]
    elif not declared:
        urls = base
    return urls

def months(a, b):
    return round((b - a).days / 30.44, 1)

def lastmod(repo_url, g, a, v):
    st, _, h = http(f"{repo_url}/{g.replace('.', '/')}/{a}/{v}/{a}-{v}.pom", "HEAD")
    if st == 200 and h.get("Last-Modified"):
        try: return email.utils.parsedate_to_datetime(h["Last-Modified"]).date()
        except Exception: return None
    return None

def osv(g, a, v):
    st, body, _ = http("https://api.osv.dev/v1/query", "POST", json.dumps({"package": {"name": f"{g}:{a}", "ecosystem": "Maven"}, "version": v}).encode())
    if st != 200: return None
    out = []
    for x in json.loads(body or "{}").get("vulns", []):
        sev = (x.get("database_specific") or {}).get("severity", "")
        out.append((x["id"], sev))
    return out

def check(c, declared, snapdir, today):
    g, a, v = c["group"], c["artifact"], c["version"]
    urls = repo_urls(c["kind"], declared)
    found, versions, latest, errors = None, [], None, 0
    for name, url in urls:
        st, blob, _ = metadata(url, g, a)
        if st == 200:
            vs, rel = parse_meta(blob)
            if name != "jitpack" or True: found, versions, latest = (name, url), vs, rel
            if snapdir:
                snapdir.mkdir(parents=True, exist_ok=True)
                (snapdir / f"{g}__{a}.xml").write_bytes(blob)
            break
        if st not in (404, 410): errors += 1
    c["repos"] = ",".join(n for n, _ in urls)
    if not found:
        if any(n == "jitpack" or "jitpack" in u for n, u in urls) and g.startswith(("com.github.", "io.github.", "com.gitlab.")):
            c["class"] = "unverifiable-jitpack"
        else:
            c["class"] = "lookup-error" if errors else "hallucinated-artifact"
        return c
    c["class"] = "ok"; c["found_in"] = found[0]
    stable = sorted([x for x in versions if is_stable(x)], key=vkey)
    c["latest_stable"] = stable[-1] if stable else None
    if v and re.fullmatch(r"[\w.\-]+", v) and versions and v not in versions:
        c["class"] = "hallucinated-version"
    if v and c["class"] == "ok" and c["latest_stable"]:
        try:
            dmaj, lmaj = int(re.match(r"\d+", v).group(0)), int(re.match(r"\d+", c["latest_stable"]).group(0))
            c["major_behind"] = lmaj - dmaj
        except Exception: c["major_behind"] = None
        d1, d2 = lastmod(found[1], g, a, v), lastmod(found[1], g, a, c["latest_stable"])
        c["gap_months"] = months(d1, d2) if d1 and d2 else None
        c["declared_released"] = str(d1) if d1 else None
        c["d02"] = bool((c["major_behind"] or 0) >= 1 or (c["gap_months"] or 0) > 12)
    if v and c["kind"] != "plugin" and re.fullmatch(r"[\w.\-]+", v) and c["class"] in ("ok",):
        vul = osv(g, a, v)
        c["vulns"] = [i for i, s in (vul or []) if s.upper() in ("HIGH", "CRITICAL")] if vul is not None else None
    return c

def run(proj_root, run_id, snapdir=None):
    coords, declared = extract(proj_root)
    today = datetime.date.today()
    with cf.ThreadPoolExecutor(8) as ex: res = list(ex.map(lambda c: check(c, declared, snapdir, today), coords))
    findings = []
    for c in res:
        ev = f"{c['group']}:{c['artifact']}:{c['version'] or '-'}"
        if c["class"] in ("hallucinated-artifact", "hallucinated-version"):
            findings.append({"run_id": run_id, "rule_id": "S11", "file": c["file"], "line": c["line"], "detector": "depcheck", "evidence": f"{c['class']} {ev} (repos: {c['repos']})", "triage": "unk"})
        if c.get("vulns"):
            findings.append({"run_id": run_id, "rule_id": "S11", "file": c["file"], "line": c["line"], "detector": "depcheck", "evidence": f"vulnerable {ev}: {','.join(c['vulns'][:3])}", "triage": "unk"})
        if c.get("d02"):
            findings.append({"run_id": run_id, "rule_id": "D02", "file": c["file"], "line": c["line"], "detector": "depcheck",
                             "evidence": f"{ev} behind latest stable {c['latest_stable']} (major_behind={c.get('major_behind')}, gap_months={c.get('gap_months')})", "triage": "unk"})
    return {"run_id": run_id, "date": str(today), "repos": sorted(declared), "coordinates": res, "findings": findings}

def main():
    ap = argparse.ArgumentParser(); ap.add_argument("cmd", choices=["scan"]); ap.add_argument("dir"); ap.add_argument("--id", required=True)
    ap.add_argument("--out"); ap.add_argument("--snapshot-dir")
    a = ap.parse_args()
    r = run(a.dir, a.id, pathlib.Path(a.snapshot_dir) if a.snapshot_dir else None)
    if a.out: pathlib.Path(a.out).write_text(json.dumps(r, indent=1, default=str))
    from collections import Counter
    print(a.id, len(r["coordinates"]), "coordinates", dict(Counter(c["class"] for c in r["coordinates"])), "findings", dict(Counter(f["rule_id"] for f in r["findings"])))

if __name__ == "__main__":
    main()
