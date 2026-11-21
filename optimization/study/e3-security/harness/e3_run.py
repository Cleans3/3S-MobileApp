#!/usr/bin/env python3
"""E3: MobSF + jadx + secret scan on one APK; writes candidate findings with automatic triage where a deterministic check exists.

  e3_run.py APK --target NAME [--package vn.x.y]       (MobSF server is started/stopped by this script; all heavy steps hold the shared bench lock)
Output: <work>/e3/<target>/{mobsf.json,jadx/} ; results/findings_candidates.csv rows: target,tool,masvs,rule_id,severity,evidence,triage,method,native_rule
triage = tp | fp | unk ; 'unk' rows are resolved by hand (triage_manual.csv) before results/findings.csv is built by build_findings.py.
tp = the described weakness is factually present in the artifact (checked in decoded manifest / decompiled code) and deviates from MASVS/MASTG guidance;
fp = factually wrong, in third-party library code that the app does not use for the flagged purpose, or the platform default flagged as a weakness.
"""
import argparse, csv, json, math, os, pathlib, re, subprocess, sys, time, urllib.request

HERE = pathlib.Path(__file__).resolve().parent
E3 = HERE.parent
sys.path.insert(0, str(HERE)); sys.path.insert(0, str(E3.parent / "e4-pressure" / "harness"))
import mobsf_scan, gen_launch as gl

WORK = gl.WORK / "e3"
MOBSF = gl.WORK / "tools" / "MobSF"
JADX = gl.WORK / "tools" / "jadx" / "bin" / "jadx"
LIBS = ("androidx/", "kotlin/", "kotlinx/", "okhttp3/", "okio/", "coil3/", "coil/", "com/google/", "defpackage/", "com/squareup/", "retrofit2/", "org/jetbrains/", "org/intellij/", "android/support/", "j$/", "META-INF/")
COLS = ["target", "tool", "masvs", "rule_id", "severity", "evidence", "triage", "method", "native_rule"]

MAP = {  # MobSF native rule -> (taxonomy id, MASVS area)
    "android_insecure_random": ("S08", "MASVS-CRYPTO"), "android_hardcoded": ("S02", "MASVS-STORAGE"), "android_logging": ("S05", "MASVS-STORAGE"),
    "android_ssl_bypass": ("S04", "MASVS-NETWORK"), "android_ssl_ignore_error": ("S04", "MASVS-NETWORK"), "android_hostname_verifier": ("S04", "MASVS-NETWORK"),
    "android_trust_all": ("S04", "MASVS-NETWORK"), "android_insecure_ssl": ("S04", "MASVS-NETWORK"), "android_webview_ignore_ssl": ("S04", "MASVS-NETWORK"),
    "android_md5": ("S08", "MASVS-CRYPTO"), "android_sha1": ("S08", "MASVS-CRYPTO"), "android_aes_ecb": ("S08", "MASVS-CRYPTO"), "android_hardcoded_iv": ("S08", "MASVS-CRYPTO"),
    "android_prevent_screenshot": ("S10", "MASVS-PLATFORM"), "android_sql_raw_query": ("S09", "MASVS-CODE"), "android_world_readable": ("S01", "MASVS-STORAGE"),
    "android_world_writable": ("S01", "MASVS-STORAGE"), "android_read_write_external": ("S01", "MASVS-STORAGE"), "android_root_detect": ("", "MASVS-RESILIENCE"),
    "app_allowbackup": ("S06", "MASVS-STORAGE"), "allowbackup": ("S06", "MASVS-STORAGE"), "app_is_debuggable": ("S12", "MASVS-RESILIENCE"), "debuggable": ("S12", "MASVS-RESILIENCE"),
    "clear_text_traffic": ("S03", "MASVS-NETWORK"), "app_clear_text_traffic": ("S03", "MASVS-NETWORK"),
    "vulnerable_os_version2": ("", "MASVS-CODE"), "vulnerable_os_version": ("", "MASVS-CODE"),
}

def server_up():
    try:
        urllib.request.urlopen(mobsf_scan.URL, timeout=3); return True
    except Exception as e:
        return getattr(e, "code", None) in (302, 301, 401, 403)

def start_server():
    if server_up(): return None
    env = {**os.environ, "TMPDIR": str(gl.WORK / "tmp" / "beta-f2-4"), "MOBSF_HOME_DIR": str(gl.WORK / "mobsf-data")}
    log = open(gl.WORK / "tools" / "mobsf-server.log", "a")
    p = subprocess.Popen([str(MOBSF / "venv" / "bin" / "python"), "manage.py", "runserver", "127.0.0.1:8000", "--noreload"], cwd=MOBSF, env=env, stdout=log, stderr=log, start_new_session=True)
    for _ in range(90):
        time.sleep(2)
        if server_up(): return p
    p.kill(); sys.exit("MobSF did not come up")

def jadx(apk, out):
    if (out / "resources" / "AndroidManifest.xml").exists(): return
    out.mkdir(parents=True, exist_ok=True)
    # -Xmx1g with -j 2 silently wrote only about half of the classes (see e3_rejadx.py); rc 3 means "finished with errors" and still writes the tree
    r = subprocess.run([str(JADX), "-j", "1", "--no-imports", "-d", str(out), str(apk)], capture_output=True, text=True, timeout=1500, env={**os.environ, "JAVA_OPTS": "-Xmx1536m"})
    if r.returncode not in (0, 3) or "OutOfMemory" in r.stdout + r.stderr: raise RuntimeError(f"jadx failed rc={r.returncode}: " + (r.stdout + r.stderr)[-300:])

def entropy(s):
    return -sum((s.count(c) / len(s)) * math.log2(s.count(c) / len(s)) for c in set(s))

SECRET_PATTERNS = [("aws-key", r"AKIA[0-9A-Z]{16}"), ("google-api-key", r"AIza[0-9A-Za-z_\-]{35}"), ("slack-token", r"xox[baprs]-[0-9A-Za-z-]{10,}"), ("github-token", r"gh[pousr]_[A-Za-z0-9]{36}"),
    ("jwt", r"eyJ[A-Za-z0-9_-]{10,}\.eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}"), ("private-key", r"-----BEGIN (?:RSA |EC |OPENSSH |)PRIVATE KEY-----"),
    ("basic-auth-url", r"://[^/\s:@\"]+:[^/\s@\"]+@"), ("bearer", r"Bearer [A-Za-z0-9._\-]{24,}"),
    ("generic-secret", r"(?i)(?:api[_-]?key|secret|passw(?:or)?d|token)[\"']?\s*[:=]\s*[\"']([A-Za-z0-9/+_\-=]{16,})[\"']")]

def secret_scan(jd, pkg):
    hits, root = [], jd
    files = [p for p in (root / "sources").rglob("*.java")] if (root / "sources").exists() else []
    app = [p for p in files if "/" + pkg.replace(".", "/") + "/" in p.as_posix() or not any(("sources/" + l) in p.as_posix() for l in LIBS)]
    res = [p for p in (root / "resources").rglob("*") if p.is_file() and p.suffix in (".xml", ".json", ".properties", ".txt") and "/res/values" in p.as_posix() or "/assets/" in p.as_posix()] if (root / "resources").exists() else []
    for p in app + res:
        try: t = p.read_text(errors="replace")
        except Exception: continue
        for name, pat in SECRET_PATTERNS:
            for m in re.finditer(pat, t):
                val = m.group(1) if m.groups() else m.group(0)
                if name == "generic-secret" and (entropy(val) < 3.3 or val.lower() in ("password", "passwordtoken")): continue
                hits.append((name, p.relative_to(root).as_posix(), val[:6] + "..." if name != "private-key" else "PEM header"))
    return hits

def manifest_text(jd):
    p = jd / "resources" / "AndroidManifest.xml"
    return p.read_text(errors="replace") if p.exists() else ""

def code_files_in_app(files, pkg):
    app = [f for f in files if f.replace("\\", "/").startswith(pkg.replace(".", "/") + "/")]
    return app

def triage(native, mapped, row, jd, pkg, files):
    """-> (tp|fp|unk, method note)"""
    mf = manifest_text(jd)
    if mapped == "S06": return ("tp", "allowBackup=true in decoded manifest") if re.search(r'allowBackup="true"', mf) and not re.search(r"dataExtractionRules|fullBackupContent", mf) else ("fp", "allowBackup not true or rules present")
    if mapped == "S12": return ("tp", "debuggable=true in decoded manifest") if 'debuggable="true"' in mf else ("fp", "no debuggable=true in manifest")
    if mapped == "S03": return ("tp", "cleartext permitted in manifest") if 'usesCleartextTraffic="true"' in mf else ("fp", "usesCleartextTraffic not true")
    if native == "vulnerable_os_version2": return ("tp", "minSdk below the supported-version recommendation (factual)")
    if mapped in ("S05", "S02", "S08", "S04") and files:
        app = code_files_in_app(files, pkg)
        if not app: return ("fp", "all matching files are third-party/library or generated code")
        return ("unk", f"{len(app)} app file(s): " + ",".join(app[:3]))
    if mapped in ("S05", "S02", "S08", "S04"): return ("unk", "no file list")
    return ("unk", "no deterministic check")

def wait_mem(min_mb=1500):
    """alpha 2026-11-21: pause before a scan while MemAvailable < 1500 MB (emulator + E1 workers share the box)"""
    while int(re.search(r"MemAvailable:\s+(\d+) kB", open("/proc/meminfo").read()).group(1)) // 1024 < min_mb: time.sleep(20)

def main():
    ap = argparse.ArgumentParser(); ap.add_argument("apk"); ap.add_argument("--target", required=True); ap.add_argument("--package")
    a = ap.parse_args(); apk = pathlib.Path(a.apk); out = WORK / a.target; out.mkdir(parents=True, exist_ok=True)
    wait_mem()
    with gl.bench_shared():
        srv = start_server()
        try:
            key = mobsf_scan.api_key()
            up = mobsf_scan.call("/api/v1/upload", files={"file": (apk.name, apk.read_bytes())}, key=key)
            rep = mobsf_scan.call("/api/v1/scan", {"hash": up["hash"], "scan_type": up["scan_type"], "file_name": up["file_name"]}, key=key)
            (out / "mobsf.json").write_text(json.dumps(rep))
        finally:
            if srv: srv.terminate(); srv.wait(30)
        jadx(apk, out / "jadx")
    pkg = a.package or rep.get("package_name", "")
    rows = []
    def add(tool, native, severity, evidence, files=None, masvs_hint=""):
        mapped, area = MAP.get(native, ("", masvs_hint or ""))
        t, why = triage(native, mapped, None, out / "jadx", pkg, files or [])
        rows.append({"target": a.target, "tool": tool, "masvs": area, "rule_id": mapped or "unmapped", "severity": severity, "evidence": evidence[:200], "triage": t, "method": why, "native_rule": native})
    for f in rep.get("manifest_analysis", {}).get("manifest_findings", []):
        if f["severity"] in ("high", "warning"): add("mobsf", f["rule"], f["severity"], f["title"])
    for f in rep.get("network_security", {}).get("network_findings", []):
        if f["severity"] in ("high", "warning"): add("mobsf", "network:" + (f.get("description") or "")[:50], f["severity"], f.get("description", ""), masvs_hint="MASVS-NETWORK")
    for k, v in rep.get("code_analysis", {}).get("findings", {}).items():
        m = v["metadata"]
        if m["severity"] in ("high", "warning") or k in ("android_logging",):
            add("mobsf", k, m["severity"], f"{m.get('description','')} [{m.get('cwe','')}]", list(v.get("files", {}).keys()), "MASVS-" + (m.get("masvs", "").split("-")[1] if "-" in m.get("masvs", "") else ""))
    for f in rep.get("certificate_analysis", {}).get("certificate_findings", []):
        if f[0] in ("high", "warning"): add("mobsf", "cert:" + f[2][:40], f[0], f[1], masvs_hint="MASVS-RESILIENCE")
    for s in rep.get("secrets", []) or []:
        add("mobsf", "secret", "high", str(s)[:160], masvs_hint="MASVS-STORAGE")
    for name, f, val in secret_scan(out / "jadx", pkg):
        rows.append({"target": a.target, "tool": "jadx", "masvs": "MASVS-STORAGE", "rule_id": "S02", "severity": "warning", "evidence": f"{name} in {f}: {val}", "triage": "unk", "method": "regex over decompiled app code/resources", "native_rule": "secret:" + name})
    p = E3 / "results" / "findings_candidates.csv"; new = not p.exists()
    with open(p, "a", newline="") as f:
        w = csv.DictWriter(f, COLS, lineterminator="\n")
        if new: w.writeheader()
        w.writerows(rows)
    from collections import Counter
    print(a.target, len(rows), dict(Counter(r["triage"] for r in rows)), flush=True)
    import e3_archive  # housekeeping: one gzip of the report + one tar.gz of the work dir, then delete the tree and MobSF's upload extraction (ntfs file-record limit)
    print(e3_archive.archive(a.target, up["hash"]), flush=True)

if __name__ == "__main__":
    main()
