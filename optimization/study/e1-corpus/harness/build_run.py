#!/usr/bin/env python3
"""Build policy R7 for one generated run, then Android Lint on it.

  build_run.py <run_id>

Step 1 zero-touch: the generated files exactly as written, plus harness-supplied non-text wrapper files (gradlew, gradle-wrapper.jar, and
gradle-wrapper.properties only if the generator wrote none; default Gradle 9.6.0, the repo's), local.properties-free (ANDROID_HOME composed SDK).
Step 2 at most 3 logged mechanical repairs (R7), each a table rule, not judgement: Gradle version bump on 'Minimum supported Gradle version',
android.useAndroidX when androidx is declared, dependency/plugin version bump to the latest stable when the declared version does not exist but
the artifact does, JDK toolchain rewrite to 17 when the requested JDK is not installed on this host (environment), and missing imports from a
fixed name -> FQN table (IMPORTS) for 'Unresolved reference' errors. Repairs touch only the build copy W/build/<run_id>, never runs/<id>/src
(analysis stays on the unrepaired source). Everything else is a fail: the first error line is recorded.
Harness environment: Gradle itself runs on JDK 17 (JAVA_HOME, a Gradle-cached Temurin: the minimum AGP supports and the level most generated files assume);
JDK 17 and 21 are both registered so toolchain requests for 17/21 resolve offline. A Kotlin/Java target mismatch ('Inconsistent JVM-target') is a
table repair (kotlin.jvm.target.validation.mode=warning), counted and logged, so the host JDK does not decide pass or fail.
Runs under flock gradle.lock, -Xmx1536m, no daemon left behind. Writes runs/<run_id>/build.json, repairs.csv rows, lint-results.xml.gz.
"""
import csv, fcntl, gzip, json, os, pathlib, re, shutil, subprocess, sys, time

HERE = pathlib.Path(__file__).resolve().parent
E1 = HERE.parent
sys.path.insert(0, str(E1 / "slop-scan"))
WORK = pathlib.Path("/mnt/01DC198BE9192500/github/Schooldev/3S-study-work")
REPO = pathlib.Path("/mnt/01DC198BE9192500/github/Schooldev/3S-MobileApp")
SDK_SRC = pathlib.Path.home() / "Android" / "Sdk"
SDK = WORK / "android-sdk"
GRADLE_HOME = WORK / "gradle-home"
DEFAULT_GRADLE = "9.6.0"
TIMEOUT = 1800
LINT_MAP = {  # Android Lint issue id -> taxonomy id (coverage matrix, protocol 5c)
    "UsesCleartextTraffic": "S03", "InsecureBaseConfiguration": "S03", "TrustAllX509TrustManager": "S04", "BadHostnameVerifier": "S04",
    "AllowAllHostnameVerifier": "S04", "CustomX509TrustManager": "S04", "SSLCertificateSocketFactoryCreateSocket": "S04",
    "SSLCertificateSocketFactoryGetInsecure": "S04", "AllowBackup": "S06", "DataExtractionRules": "S06", "FullBackupContent": "S06",
    "ExportedActivity": "S07", "ExportedService": "S07", "ExportedReceiver": "S07", "ExportedContentProvider": "S07", "UnprotectedSMSBroadcastReceiver": "S07",
    "GetInstance": "S08", "DeprecatedProvider": "S08", "TrulyRandom": "S08", "SetJavaScriptEnabled": "S09", "JavascriptInterface": "S09",
    "Deprecated": "D01", "GradleDeprecated": "D01", "GradleDeprecatedConfiguration": "D01",
    "GradleDependency": "D02", "NewerVersionAvailable": "D02", "AndroidGradlePluginVersion": "D02", "OldTargetApi": "D02",
    "FrequentlyChangingValue": "P05", "ContentDescription": "U01",
}

IMPORTS = {  # name -> FQN, only names a reader would call unambiguous; anything else stays a build failure
    **{n: "androidx.compose.foundation.layout." + n for n in (
        "fillMaxSize", "fillMaxWidth", "fillMaxHeight", "padding", "size", "height", "width", "heightIn", "widthIn", "sizeIn", "offset",
        "wrapContentSize", "wrapContentWidth", "wrapContentHeight", "aspectRatio", "Spacer", "Column", "Row", "Box", "Arrangement", "PaddingValues",
        "systemBarsPadding", "navigationBarsPadding", "statusBarsPadding", "imePadding", "safeDrawingPadding")},
    **{n: "androidx.compose.foundation." + n for n in ("background", "clickable", "border", "BorderStroke", "verticalScroll", "rememberScrollState", "horizontalScroll")},
    **{n: "androidx.compose.foundation.lazy." + n for n in ("LazyColumn", "LazyRow", "items", "itemsIndexed", "rememberLazyListState", "LazyListState")},
    **{n: "androidx.compose.foundation.shape." + n for n in ("RoundedCornerShape", "CircleShape")},
    **{n: "androidx.compose.runtime." + n for n in (
        "remember", "mutableStateOf", "getValue", "setValue", "collectAsState", "LaunchedEffect", "rememberSaveable", "Composable",
        "derivedStateOf", "mutableStateListOf", "DisposableEffect", "SideEffect", "rememberCoroutineScope", "mutableIntStateOf")},
    "Modifier": "androidx.compose.ui.Modifier", "Alignment": "androidx.compose.ui.Alignment",
    "dp": "androidx.compose.ui.unit.dp", "sp": "androidx.compose.ui.unit.sp",
    "FontWeight": "androidx.compose.ui.text.font.FontWeight", "TextOverflow": "androidx.compose.ui.text.style.TextOverflow",
    "TextAlign": "androidx.compose.ui.text.style.TextAlign", "Color": "androidx.compose.ui.graphics.Color",
    "ContentScale": "androidx.compose.ui.layout.ContentScale", "clip": "androidx.compose.ui.draw.clip", "alpha": "androidx.compose.ui.draw.alpha",
    "collectAsStateWithLifecycle": "androidx.lifecycle.compose.collectAsStateWithLifecycle",
    "viewModel": "androidx.lifecycle.viewmodel.compose.viewModel",
}
JDK_PATHS = [str(p) for p in sorted((pathlib.Path.home() / ".gradle" / "jdks").glob("*adoptium-17*/")) if p.is_dir()] + ["/usr/lib/jvm/java-21-openjdk-amd64"]

def fix_imports(dst, out):
    """Add table imports for 'Unresolved reference' / missing property-delegate errors, in the build copy only. Returns what was added."""
    applied, seen = [], set()
    for m in re.finditer(r"(?m)^e: (?:file://)?(\S+?\.kt):\d+:\d+ (?:Unresolved reference '(\w+)|Property delegate must have a '(getValue|setValue)\(\.\.\.\)' method)", out):
        path, name = m.group(1), m.group(2) or m.group(3)
        for n in (("getValue", "setValue") if name in ("getValue", "setValue") else (name,)):
            fq = IMPORTS.get(n)
            p = pathlib.Path(path)
            if not fq or (path, fq) in seen or not p.exists() or str(dst) not in path: continue
            seen.add((path, fq)); t = p.read_text()
            if re.search(r"(?m)^import " + re.escape(fq) + r"\s*$", t): continue
            line = "import " + fq + "\n"
            t = re.sub(r"(?m)^(package .*\n)", lambda x: x.group(1) + "\n" + line, t, count=1) if re.search(r"(?m)^package ", t) else line + t
            p.write_text(t); applied.append(f"{p.name}:{n}")
    return applied

def prune_gradle_home():
    """The work volume is NTFS and runs out of file records (ENOSPC with 300+ GB free, 2026-11-21): artifact-transform outputs and compiled
    build-script caches are ~4k files per build and regenerable, so they go after every build. Dependency jars (modules-2) stay."""
    caches = GRADLE_HOME / "caches"
    for p in list(caches.glob("transforms-*")) + list(caches.glob("*/transforms")) + list(caches.glob("*/kotlin-dsl")) + list(caches.glob("*/groovy-dsl")) + list(caches.glob("*/dependencies-accessors")):
        shutil.rmtree(p, ignore_errors=True)

def compose_sdk():
    """Writable SDK on /mnt: symlinks to the installed platforms/build-tools, so AGP can auto-download others without touching the root disk."""
    for sub in ("platforms", "build-tools"):
        (SDK / sub).mkdir(parents=True, exist_ok=True)
        for d in (SDK_SRC / sub).iterdir():
            if not (SDK / sub / d.name).exists(): (SDK / sub / d.name).symlink_to(d)
    for sub in ("platform-tools", "cmdline-tools", "emulator"):
        if (SDK_SRC / sub).exists() and not (SDK / sub).exists(): (SDK / sub).symlink_to(SDK_SRC / sub)
    if not (SDK / "licenses").exists(): shutil.copytree(SDK_SRC / "licenses", SDK / "licenses")

def env():
    e = {k: v for k, v in os.environ.items() if not k.startswith(("CLAUDE", "HERDR", "CODEX", "GEMINI"))}
    e.update({"JAVA_HOME": JDK_PATHS[0], "GRADLE_USER_HOME": str(GRADLE_HOME), "ANDROID_HOME": str(SDK), "ANDROID_SDK_ROOT": str(SDK),
              "TMPDIR": str(WORK / "tmp" / "beta-f2-2"), "XDG_CACHE_HOME": str(WORK / "xdg-cache"), "JAVA_OPTS": "-Xmx512m"})
    return e

def prepare(src, dst):
    shutil.rmtree(dst, ignore_errors=True); shutil.copytree(src, dst)
    (dst / "gradle" / "wrapper").mkdir(parents=True, exist_ok=True)
    for f in ("gradlew", "gradlew.bat"): shutil.copyfile(REPO / "android" / f, dst / f)
    os.chmod(dst / "gradlew", 0o755)
    shutil.copyfile(REPO / "android" / "gradle" / "wrapper" / "gradle-wrapper.jar", dst / "gradle" / "wrapper" / "gradle-wrapper.jar")
    prop = dst / "gradle" / "wrapper" / "gradle-wrapper.properties"
    if not prop.exists():
        prop.write_text(f"distributionBase=GRADLE_USER_HOME\ndistributionPath=wrapper/dists\ndistributionUrl=https\\://services.gradle.org/distributions/gradle-{DEFAULT_GRADLE}-bin.zip\nzipStoreBase=GRADLE_USER_HOME\nzipStorePath=wrapper/dists\n")
    gp = dst / "gradle.properties"                       # heap cap + no daemon are harness resource limits, not repairs
    t = gp.read_text() if gp.exists() else ""
    t = re.sub(r"(?m)^(org\.gradle\.jvmargs|kotlin\.daemon\.jvmargs|org\.gradle\.daemon|org\.gradle\.parallel|org\.gradle\.caching)=.*\n?", "", t)
    gp.write_text(t.rstrip("\n") + "\norg.gradle.jvmargs=-Xmx1536m\nkotlin.daemon.jvmargs=-Xmx1024m\norg.gradle.daemon=false\n"
                  + "org.gradle.java.installations.paths=" + ",".join(JDK_PATHS) + "\norg.gradle.java.installations.auto-download=false\n")

def gradle(dst, tasks):
    with open(WORK / "gradle.lock", "a") as lk:
        fcntl.flock(lk, fcntl.LOCK_EX)
        t0 = time.time()
        p = subprocess.run(["./gradlew", "--no-daemon", "--console=plain", "--continue", *tasks], cwd=dst, env=env(), capture_output=True, text=True, timeout=TIMEOUT)
        subprocess.run(["./gradlew", "--stop"], cwd=dst, env=env(), capture_output=True, timeout=120)
    return p.returncode, p.stdout + p.stderr, round(time.time() - t0, 1)

def first_error(out):
    m = re.search(r"^e: (?:file://)?(.+)$", out, re.M)
    if m: return m.group(0)[:200]
    m = re.search(r"What went wrong:\s*\n(.+)", out)
    if m: return m.group(1).strip()[:200]
    m = re.search(r"^(?:FAILURE|Error|error:).*$", out, re.M)
    return (m.group(0) if m else out.strip().splitlines()[-1] if out.strip() else "")[:200]

def repair(dst, out):
    """One mechanical repair from the table, or None."""
    m = re.search(r"Minimum supported Gradle version is ([\d.]+)", out)
    if m:
        p = dst / "gradle/wrapper/gradle-wrapper.properties"
        p.write_text(re.sub(r"gradle-[\d.]+(-\w+)?-bin\.zip", f"gradle-{m.group(1)}-bin.zip", p.read_text()))
        return ("gradle-version", f"wrapper -> {m.group(1)}")
    if "android.useAndroidX" in out and "gradle.properties" in out:
        gp = dst / "gradle.properties"
        gp.write_text((gp.read_text() if gp.exists() else "").rstrip("\n") + "\nandroid.useAndroidX=true\n")
        return ("gradle-properties", "android.useAndroidX=true (androidx dependencies declared, flag missing)")
    m = re.search(r"Cannot find a Java installation .*?languageVersion=(\d+)", out)
    if m and m.group(1) not in ("17", "21"):
        n = 0
        for f in list(dst.rglob("*.gradle*")):
            t = f.read_text(); t2 = re.sub(r"(jvmToolchain\(\s*)\d+(\s*\))", r"\g<1>17\2", t); t2 = re.sub(r"(JavaLanguageVersion\.of\(\s*)\d+(\s*\))", r"\g<1>17\2", t2)
            if t2 != t: f.write_text(t2); n += 1
        if n: return ("jdk-toolchain", f"requested JDK {m.group(1)} not installed -> 17 in {n} build file(s)")
    if "Inconsistent JVM-target compatibility" in out:
        gp = dst / "gradle.properties"
        gp.write_text((gp.read_text() if gp.exists() else "").rstrip("\n") + "\nkotlin.jvm.target.validation.mode=warning\n")
        return ("jvm-target-validation", "kotlin.jvm.target.validation.mode=warning (Kotlin and Java targets differ)")
    imp = fix_imports(dst, out)
    if imp: return ("missing-import", f"{len(imp)} imports: " + ", ".join(imp[:15]))
    m = re.search(r"Could not find ([\w.\-]+):([\w.\-]+):([\w.\-]+)\.", out) or re.search(r"Could not resolve ([\w.\-]+):([\w.\-]+):([\w.\-]+)", out)
    if m:
        import depcheck
        g, a, v = m.groups()
        st, blob, _ = depcheck.metadata(depcheck.REPOS["google"], g, a)
        if st != 200: st, blob, _ = depcheck.metadata(depcheck.REPOS["mavenCentral"], g, a)
        if st == 200:
            vs, rel = depcheck.parse_meta(blob)
            stable = sorted([x for x in vs if depcheck.is_stable(x)], key=depcheck.vkey)
            if stable and v not in vs:
                n = 0
                for f in list(dst.rglob("*.gradle*")) + list(dst.rglob("*.toml")):
                    t = f.read_text(); t2 = t.replace(f"{g}:{a}:{v}", f"{g}:{a}:{stable[-1]}")
                    if t2 == t and f.suffix == ".toml": t2 = re.sub(r'(?m)^(\s*[\w\-]+\s*=\s*")' + re.escape(v) + '"', lambda x: x.group(1) + stable[-1] + '"', t)
                    if t2 != t: f.write_text(t2); n += 1
                if n: return ("version-bump", f"{g}:{a} {v} -> {stable[-1]} (declared version does not exist)")
    return None

def lint_rows(xmlp, run_id):
    import xml.etree.ElementTree as ET
    rows = []
    for i in ET.parse(xmlp).getroot().iter("issue"):
        iid = i.get("id"); loc = i.find("location")
        rows.append({"run_id": run_id, "rule_id": LINT_MAP.get(iid, "lint:" + iid), "file": (loc.get("file") if loc is not None else "") or "", "line": (loc.get("line") if loc is not None else "") or "",
                     "detector": "lint", "evidence": (iid + ": " + (i.get("message") or ""))[:160], "triage": "unk"})
    return rows

def main():
    rid = sys.argv[1]
    run = E1 / "runs" / rid
    meta = json.loads((run / "meta.json").read_text())
    if not meta.get("valid"):
        (run / "build.json").write_text(json.dumps({"build": "skip", "build_error_first_line": "invalid run (no .kt or no Gradle file)"})); return
    compose_sdk(); GRADLE_HOME.mkdir(exist_ok=True)
    dst = WORK / "build" / rid; prepare(run / "src", dst)
    repairs, bj = [], {"build_zero_touch": "fail", "build_repaired": "", "repairs": 0}
    tasks = ["assembleDebug", "lintDebug"]
    rc, out, dur = gradle(dst, tasks)
    apk = next(iter((dst).rglob("outputs/apk/debug/*.apk")), None)
    bj["build_zero_touch"] = "pass" if apk else "fail"
    bj["gradle_s_first"] = dur
    err = "" if apk else first_error(out)
    step, done_kinds = 0, set()
    while not apk and step < 3:
        r = repair(dst, out)
        if not r or (r[0] in ("gradle-properties", "jdk-toolchain", "jvm-target-validation") and r[0] in done_kinds): break   # a property repair that did not clear its error is not repeated
        done_kinds.add(r[0])
        step += 1; repairs.append({"run_id": rid, "step": step, "kind": r[0], "detail": r[1]})
        rc, out, dur = gradle(dst, tasks)
        apk = next(iter((dst).rglob("outputs/apk/debug/*.apk")), None)
        err = "" if apk else first_error(out)
    bj.update({"build": "pass" if apk else "fail", "build_repaired": ("pass" if apk else "fail") if repairs else "", "repairs": len(repairs), "build_error_first_line": err})
    if apk:
        (WORK / "apk-corpus").mkdir(exist_ok=True); shutil.copyfile(apk, WORK / "apk-corpus" / f"{rid}.apk")
    lint = next(iter(dst.rglob("reports/lint-results-debug.xml")), None)
    if lint:
        with open(lint, "rb") as f, gzip.open(run / "lint-results.xml.gz", "wb") as g: g.write(f.read())
        rows = lint_rows(lint, rid); bj["lint"] = "ran"; bj["lint_issues"] = len(rows)
        with open(run / "lint_findings.json", "w") as f: json.dump(rows, f)
    else:
        bj["lint"] = "no-report"
    with gzip.open(run / "gradle-log.txt.gz", "wt") as f: f.write(out[-200000:])
    (run / "build.json").write_text(json.dumps(bj, indent=1))
    if repairs:
        rp = E1 / "results" / "repairs.csv"; new = not rp.exists()
        with open(rp, "a", newline="") as f:
            w = csv.DictWriter(f, ["run_id", "step", "kind", "detail"]);
            if new: w.writeheader()
            w.writerows(repairs)
    prune_gradle_home()
    shutil.rmtree(dst, ignore_errors=True)               # the build copy holds ~500 files; the volume is NTFS and file-record exhaustion causes ENOSPC (2026-11-21), so nothing is kept
    print(rid, bj["build"], "zero-touch", bj["build_zero_touch"], "repairs", len(repairs), err[:100])

if __name__ == "__main__":
    main()
