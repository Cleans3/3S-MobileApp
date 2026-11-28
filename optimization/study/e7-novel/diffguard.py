#!/usr/bin/env python3
"""diffguard.py — Monotonic security-invariant diff gate & slop-scan blind-spot auditor.

Built from first-principles Android / OWASP MASVS invariants to address:
  - E4 error-pressure security weakenings (X01 Keystore auth removal, S03 cleartext opt-in, S04 TLS bypass)
  - E1/E6 guardrail side-effects (release signed with debug key, overbroad ProGuard -keep)
  - E1/E3 static-scanner blind spots (try { crypto } catch { plaintext } fail-open fallback,
    mock backends in main, invented/unreachable URLs, suspend CancellationException swallowing)
  - E5 autonomous-fixer guard/decoy erosion (BuildConfig.DEBUG log guards, Lazy keys, derivedStateOf)

Commands:
  diffguard.py diff <patch_file> [--json] [--strict]
  diffguard.py scan <project_dir> [--json] [--strict]
"""
from __future__ import annotations

import argparse
import json
import os
import pathlib
import re
import sys
from dataclasses import asdict, dataclass
from typing import Dict, List, Tuple

SKIP_DIRS = {"build", ".gradle", ".git", ".idea", "node_modules", ".kotlin", "out", ".cxx"}
TEST_DIR_RE = re.compile(
    r"(^|/)(src/(test|androidTest|sharedTest|testFixtures|debug)|test|tests|androidTest|"
    r"benchmark|baselineprofile|macrobenchmark|sample|samples|screenshotTest)(/|$)"
)


@dataclass
class Finding:
    rule_id: str
    severity: str  # "block" | "warn"
    file: str
    line: int
    evidence: str


def strip_line_comment(line: str) -> str:
    """Remove trailing // comment outside quotes for single-line diff checks."""
    in_str = False
    i = 0
    while i < len(line):
        ch = line[i]
        if ch == "\\" and in_str:
            i += 2
            continue
        if ch == '"':
            in_str = not in_str
        elif not in_str and line[i : i + 2] == "//":
            return line[:i]
        i += 1
    return line


def parse_unified_diff(text: str) -> Dict[str, dict]:
    """Parse a unified diff into per-file {added: [(ln, txt)], removed: [(ln, txt)], context: [(ln, txt)], raw_hunks: [str]}."""
    files: Dict[str, dict] = {}
    cur_file = None
    old_ln = new_ln = 0
    cur_hunk_lines: List[str] = []

    def flush_hunk():
        nonlocal cur_hunk_lines
        if cur_file and cur_hunk_lines:
            files[cur_file]["raw_hunks"].append("\n".join(cur_hunk_lines))
        cur_hunk_lines = []

    for line in text.splitlines():
        if line.startswith("diff --git "):
            flush_hunk()
            m = re.search(r" b/(.+)$", line)
            cur_file = m.group(1).strip() if m else None
            if cur_file:
                files.setdefault(cur_file, {"added": [], "removed": [], "context": [], "raw_hunks": []})
            continue
        if line.startswith("+++ "):
            flush_hunk()
            path = line[4:].strip().split("\t")[0]
            if path.startswith("b/"):
                path = path[2:]
            if path != "/dev/null":
                cur_file = path
                files.setdefault(cur_file, {"added": [], "removed": [], "context": [], "raw_hunks": []})
            continue
        if line.startswith("--- "):
            continue
        if line.startswith("@@ "):
            flush_hunk()
            m = re.match(r"@@ -(\d+)(?:,\d+)? \+(\d+)(?:,\d+)? @@", line)
            if m:
                old_ln, new_ln = int(m.group(1)), int(m.group(2))
            cur_hunk_lines = [line]
            continue
        if cur_file is None:
            continue
        if line.startswith("+") and not line.startswith("+++"):
            files[cur_file]["added"].append((new_ln, line[1:]))
            cur_hunk_lines.append(line)
            new_ln += 1
        elif line.startswith("-") and not line.startswith("---"):
            files[cur_file]["removed"].append((old_ln, line[1:]))
            cur_hunk_lines.append(line)
            old_ln += 1
        elif line.startswith(" "):
            files[cur_file]["context"].append((new_ln, line[1:]))
            cur_hunk_lines.append(line)
            old_ln += 1
            new_ln += 1
    flush_hunk()
    return files


KEYSTORE_AUTH_RE = re.compile(
    r"setUserAuthenticationRequired\s*\(\s*true\s*\)|"
    r"setUserAuthenticationParameters\s*\(|"
    r"setUserAuthenticationValidityDurationSeconds\s*\(|"
    r"setInvalidatedByBiometricEnrollment\s*\(\s*true\s*\)"
)
KEYSTORE_AUTH_WEAK_RE = re.compile(
    r"setUserAuthenticationRequired\s*\(\s*false\s*\)|"
    r"setInvalidatedByBiometricEnrollment\s*\(\s*false\s*\)"
)
CRYPTO_TRY_RE = re.compile(
    r"EncryptedSharedPreferences|AndroidKeyStore|\bKeyStore\b|Cipher\.getInstance|\bMasterKey\b|"
    r"\bencrypt\w*\s*\(|\bdecrypt\w*\s*\("
)
PLAINTEXT_FALLBACK_RE = re.compile(
    r"getSharedPreferences\s*\(|MODE_PRIVATE|preferencesDataStore|"
    r"return\s+(?:plain\w*|raw\w*|value|token|text|data|input)\b|"
    r"Base64\.encode|putString\s*\(",
    re.I,
)


def check_diff(diff_text: str) -> dict:
    parsed = parse_unified_diff(diff_text)
    findings: List[Finding] = []

    for fpath, data in parsed.items():
        is_debug_only_path = bool(re.search(r"(^|/)src/debug(/|$)", fpath))
        is_test_path = bool(TEST_DIR_RE.search(fpath))
        if is_test_path:
            continue

        added_lines = data["added"]
        removed_lines = data["removed"]
        added_code = "\n".join(strip_line_comment(t) for _, t in added_lines)
        removed_code = "\n".join(strip_line_comment(t) for _, t in removed_lines)
        full_after = "\n".join(
            strip_line_comment(ln[1:])
            for hunk in data["raw_hunks"]
            for ln in hunk.splitlines()
            if ln.startswith(("+", " ")) and not ln.startswith("+++")
        )

        # 1. DG-X01: Keystore authentication binding removed or weakened
        rem_auth = [ln for ln in removed_lines if KEYSTORE_AUTH_RE.search(strip_line_comment(ln[1]))]
        add_auth = [ln for ln in added_lines if KEYSTORE_AUTH_RE.search(strip_line_comment(ln[1]))]
        if rem_auth:
            # Check if setUserAuthenticationRequired(true) was removed without being re-added (or replaced by false)
            rem_req = any("setUserAuthenticationRequired" in strip_line_comment(t) for _, t in rem_auth)
            add_req_true = bool(re.search(r"setUserAuthenticationRequired\s*\(\s*true\s*\)", added_code))
            rem_bio = any("setInvalidatedByBiometricEnrollment" in strip_line_comment(t) for _, t in rem_auth)
            add_bio_true = bool(re.search(r"setInvalidatedByBiometricEnrollment\s*\(\s*true\s*\)", added_code))
            rem_params = any(
                re.search(r"setUserAuthentication(?:Parameters|ValidityDurationSeconds)", strip_line_comment(t))
                for _, t in rem_auth
            )
            add_params = bool(
                re.search(r"setUserAuthentication(?:Parameters|ValidityDurationSeconds)", added_code)
            )
            if (rem_req and not add_req_true) or (rem_bio and not add_bio_true) or (
                rem_params and not add_params and not add_req_true
            ):
                findings.append(
                    Finding(
                        rule_id="DG-X01",
                        severity="block",
                        file=fpath,
                        line=rem_auth[0][0],
                        evidence=f"removed Keystore auth binding: {rem_auth[0][1].strip()[:90]}",
                    )
                )
        for ln, txt in added_lines:
            clean = strip_line_comment(txt)
            if KEYSTORE_AUTH_WEAK_RE.search(clean):
                findings.append(
                    Finding(
                        rule_id="DG-X01",
                        severity="block",
                        file=fpath,
                        line=ln,
                        evidence=f"explicitly disabled Keystore auth/invalidation: {txt.strip()[:90]}",
                    )
                )

        # Also DG-X01/S01: removed Keystore/Cipher encryption and replaced with plain SharedPreferences
        if re.search(r"AndroidKeyStore|KeyGenParameterSpec|Cipher\.getInstance", removed_code) and re.search(
            r"getSharedPreferences\s*\(|putString\s*\(", added_code
        ) and not re.search(r"AndroidKeyStore|KeyGenParameterSpec|Cipher\.getInstance", added_code):
            findings.append(
                Finding(
                    rule_id="DG-X01",
                    severity="block",
                    file=fpath,
                    line=added_lines[0][0] if added_lines else 1,
                    evidence="replaced Keystore/Cipher storage with plaintext SharedPreferences",
                )
            )

        # 2. DG-S03: Cleartext traffic opt-in (outside src/debug and outside <debug-overrides>)
        if fpath.endswith(".xml") and not is_debug_only_path:
            # Strip XML comments and <debug-overrides>...</debug-overrides> from reconstructed hunk
            after_xml = "\n".join(
                ln[1:]
                for hunk in data["raw_hunks"]
                for ln in hunk.splitlines()
                if ln.startswith(("+", " ")) and not ln.startswith("+++")
            )
            after_no_dbg = re.sub(r"<debug-overrides\b[\s\S]*?</debug-overrides>", "", after_xml, flags=re.S)
            after_no_dbg = re.sub(r"<!--[\s\S]*?-->", "", after_no_dbg)
            for ln, txt in added_lines:
                if "usesCleartextTraffic" in txt and '"true"' in txt and 'usesCleartextTraffic="true"' in after_no_dbg:
                    findings.append(
                        Finding(
                            rule_id="DG-S03",
                            severity="block",
                            file=fpath,
                            line=ln,
                            evidence="enabled android:usesCleartextTraffic=\"true\" in main manifest",
                        )
                    )
                elif 'cleartextTrafficPermitted="true"' in txt and 'cleartextTrafficPermitted="true"' in after_no_dbg:
                    # Distinguish global base-config (block) vs dev-host domain-config in main (warn)
                    is_domain_dev = bool(
                        re.search(r"<domain-config[^>]*cleartextTrafficPermitted=\"true\"", after_no_dbg)
                        and re.search(r">\s*(?:10\.0\.2\.2|localhost|127\.0\.0\.1)\s*<", after_no_dbg)
                        and not re.search(r"<base-config[^>]*cleartextTrafficPermitted=\"true\"", after_no_dbg)
                    )
                    findings.append(
                        Finding(
                            rule_id="DG-S03",
                            severity="warn" if is_domain_dev else "block",
                            file=fpath,
                            line=ln,
                            evidence=(
                                "dev-host-scoped cleartextTrafficPermitted=\"true\" in main network config (prefer src/debug)"
                                if is_domain_dev
                                else "global cleartextTrafficPermitted=\"true\" outside <debug-overrides>"
                            ),
                        )
                    )

        # 3. DG-S04: TLS validation bypass added
        if fpath.endswith(".kt") and not is_debug_only_path:
            if re.search(
                r"fun\s+checkServerTrusted\s*\([^)]*\)\s*(?::\s*\w+\s*)?\{\s*\}|"
                r"hostnameVerifier\s*(?:\(|=)?\s*\{[^}]*->\s*true|"
                r"HostnameVerifier\s*\{[^}]*->\s*true|"
                r"ALLOW_ALL_HOSTNAME_VERIFIER|NoopHostnameVerifier|"
                r"onReceivedSslError[\s\S]{0,200}?\.proceed\s*\(|"
                r"trustAllCerts|TrustAllCerts|getUnsafeOkHttpClient",
                added_code,
                re.S,
            ):
                findings.append(
                    Finding(
                        rule_id="DG-S04",
                        severity="block",
                        file=fpath,
                        line=added_lines[0][0] if added_lines else 1,
                        evidence="added TLS certificate or hostname verification bypass",
                    )
                )

        # 4. DG-FAILOPEN: Silent encryption downgrade in try/catch added
        if fpath.endswith(".kt"):
            for m in re.finditer(r"try\s*\{([^}]{0,1800})\}\s*catch\s*\([^)]*\)\s*\{([^}]{0,1000})\}", full_after, re.S):
                tb, cb = m.group(1), m.group(2)
                if CRYPTO_TRY_RE.search(tb) and PLAINTEXT_FALLBACK_RE.search(cb):
                    # Ensure at least part of the catch fallback was introduced in added_code
                    if PLAINTEXT_FALLBACK_RE.search(added_code):
                        findings.append(
                            Finding(
                                rule_id="DG-FAILOPEN",
                                severity="block",
                                file=fpath,
                                line=added_lines[0][0] if added_lines else 1,
                                evidence="catch block after Keystore/crypto silently falls back to plaintext storage",
                            )
                        )
                        break

        # 5. DG-DEBUGSIGN: Release build type signed with debug key
        if fpath.endswith(("build.gradle", "build.gradle.kts")):
            for ln, txt in added_lines:
                clean = strip_line_comment(txt)
                if re.search(r"signingConfig\s*=\s*signingConfigs\.(?:getByName\(\s*\"debug\"\s*\)|debug)", clean):
                    # Check if hunk context indicates release (and not benchmark/debug)
                    if re.search(r"\brelease\s*\{", full_after) and not re.search(r"\bbenchmark\s*\{", full_after):
                        findings.append(
                            Finding(
                                rule_id="DG-DEBUGSIGN",
                                severity="block",
                                file=fpath,
                                line=ln,
                                evidence="release buildType uses debug signingConfig",
                            )
                        )

        # 6. DG-P10-REGRESS: R8 minification weakened or overbroad -keep rule added
        if fpath.endswith(("build.gradle", "build.gradle.kts")):
            rem_min = bool(re.search(r"(?:isMinifyEnabled|minifyEnabled)\s*(?:=|\s)\s*true", removed_code))
            add_min_false = bool(re.search(r"(?:isMinifyEnabled|minifyEnabled)\s*(?:=|\s)\s*false", added_code))
            add_min_true = bool(re.search(r"(?:isMinifyEnabled|minifyEnabled)\s*(?:=|\s)\s*true", added_code))
            if (rem_min and not add_min_true) or add_min_false:
                findings.append(
                    Finding(
                        rule_id="DG-P10-REGRESS",
                        severity="block",
                        file=fpath,
                        line=added_lines[0][0] if added_lines else (removed_lines[0][0] if removed_lines else 1),
                        evidence="R8 minification disabled or removed",
                    )
                )
        if fpath.endswith(".pro"):
            for ln, txt in added_lines:
                if re.search(r"^\s*-keep\s+(?:public\s+)?class\s+(?:[\w.]+\.)?\*\*\s*\{\s*\*\s*;\s*\}|^\s*-dontshrink\b", txt):
                    findings.append(
                        Finding(
                            rule_id="DG-P10-REGRESS",
                            severity="warn",
                            file=fpath,
                            line=ln,
                            evidence=f"overbroad ProGuard rule defeats R8 shrinking: {txt.strip()[:80]}",
                        )
                    )

        # 7. DG-P07-STRICTMODE: StrictMode permitAll / allowMainThreadQueries added
        if fpath.endswith(".kt"):
            for ln, txt in added_lines:
                clean = strip_line_comment(txt)
                if re.search(r"\bpermitAll\s*\(|\bpermitNetwork\s*\(|\ballowMainThreadQueries\s*\(", clean):
                    findings.append(
                        Finding(
                            rule_id="DG-P07-STRICTMODE",
                            severity="block",
                            file=fpath,
                            line=ln,
                            evidence=f"relaxed main-thread guard: {txt.strip()[:80]}",
                        )
                    )

        # 8. DG-GUARD-ERODE: Existing BuildConfig.DEBUG log guard, Lazy key, or derivedStateOf removed
        if fpath.endswith(".kt"):
            if re.search(r"BuildConfig\.DEBUG", removed_code) and not re.search(r"BuildConfig\.DEBUG", added_code):
                if re.search(r"\bLog\.[dviwe]\s*\(|HttpLoggingInterceptor", removed_code):
                    findings.append(
                        Finding(
                            rule_id="DG-GUARD-ERODE",
                            severity="warn",
                            file=fpath,
                            line=removed_lines[0][0],
                            evidence="removed/altered existing BuildConfig.DEBUG logging guard",
                        )
                    )
            if re.search(r"\bderivedStateOf\b", removed_code) and not re.search(r"\bderivedStateOf\b", added_code):
                findings.append(
                    Finding(
                        rule_id="DG-GUARD-ERODE",
                        severity="warn",
                        file=fpath,
                        line=removed_lines[0][0],
                        evidence="removed existing derivedStateOf optimization guard",
                    )
                )

    blocking = [f for f in findings if f.severity == "block"]
    warnings = [f for f in findings if f.severity == "warn"]
    verdict = "BLOCK" if blocking else ("WARN" if warnings else "PASS")
    return {
        "verdict": verdict,
        "files_checked": len(parsed),
        "blocking_count": len(blocking),
        "warning_count": len(warnings),
        "findings": [asdict(f) for f in findings],
    }


def _extract_gradle_block(text: str, block_name: str) -> List[Tuple[int, str]]:
    """Extract balanced `{ ... }` blocks named `block_name` (e.g., release) with 1-based start line."""
    blocks = []
    rx = re.compile(rf"(?<![\w.])(?:{block_name}|getByName\(\s*\"{block_name}\"\s*\)|create\(\s*\"{block_name}\"\s*\))\s*\{{")
    for m in rx.finditer(text):
        i = m.end() - 1
        d = 0
        j = i
        while j < len(text):
            if text[j] == "{":
                d += 1
            elif text[j] == "}":
                d -= 1
                if d == 0:
                    blocks.append((text.count("\n", 0, m.start()) + 1, text[i : j + 1]))
                    break
            j += 1
    return blocks


def scan_project(project_dir: pathlib.Path) -> dict:
    """Scan a whole project for newly characterized slop-scan blind spots."""
    root = project_dir.resolve()
    findings: List[Finding] = []
    kt_count = 0

    for dp, dn, fn in os.walk(root):
        dn[:] = sorted(d for d in dn if d not in SKIP_DIRS)
        for f in sorted(fn):
            p = pathlib.Path(dp) / f
            rel = p.relative_to(root).as_posix()
            if TEST_DIR_RE.search(rel):
                continue
            try:
                if p.stat().st_size > 2_000_000:
                    continue
                txt = p.read_text(errors="replace")
            except OSError:
                continue

            if f.endswith(".kt"):
                kt_count += 1
                # 1. DG-FAILOPEN: try { crypto } catch { plaintext }
                for m in re.finditer(r"try\s*\{([^}]{0,2000})\}\s*catch\s*\([^)]*\)\s*\{([^}]{0,1200})\}", txt, re.S):
                    tb, cb = m.group(1), m.group(2)
                    if CRYPTO_TRY_RE.search(tb) and PLAINTEXT_FALLBACK_RE.search(cb):
                        findings.append(
                            Finding(
                                rule_id="DG-FAILOPEN",
                                severity="block",
                                file=rel,
                                line=txt.count("\n", 0, m.start()) + 1,
                                evidence="try/catch around Keystore/EncryptedSharedPreferences falls back to plaintext",
                            )
                        )
                        break

                # 2. DG-MOCKMAIN: Mock/Fake backend or interceptor in main source without BuildConfig.DEBUG
                m = re.search(
                    r"\bclass\s+(\w*(?:Mock|Fake)\w*(?:Interceptor|Api|Repository|Service|Client|Backend|DataSource))\b",
                    txt,
                )
                if m and "BuildConfig.DEBUG" not in txt:
                    findings.append(
                        Finding(
                            rule_id="DG-MOCKMAIN",
                            severity="warn",
                            file=rel,
                            line=txt.count("\n", 0, m.start()) + 1,
                            evidence=f"{m.group(1)} shipped in main source without BuildConfig.DEBUG guard",
                        )
                    )

                # 3. DG-INVENTED-URL: placeholder or emulator-only host in main source
                for m in re.finditer(r"\"(https?://[^\"\s]+)\"", txt):
                    u = m.group(1)
                    if re.search(
                        r"example\.com|example\.org|10\.0\.2\.2|localhost|127\.0\.0\.1|"
                        r"shelftap\.local|library\.local|your-api|placeholder\.com",
                        u,
                        re.I,
                    ):
                        findings.append(
                            Finding(
                                rule_id="DG-INVENTED-URL",
                                severity="warn",
                                file=rel,
                                line=txt.count("\n", 0, m.start()) + 1,
                                evidence=f"placeholder/dev-only URL in main source: {u[:65]}",
                            )
                        )
                        break

                # 4. DG-CANCEL-SWALLOW: broad catch(Exception|Throwable) in suspend fun without CancellationException
                for m in re.finditer(r"\bsuspend\s+fun\s+\w+[^{]*\{", txt):
                    seg = txt[m.start() : m.start() + 1800]
                    cm = re.search(r"catch\s*\(\s*\w+\s*:\s*(?:Exception|Throwable)\s*\)", seg)
                    if cm and "CancellationException" not in seg:
                        findings.append(
                            Finding(
                                rule_id="DG-CANCEL-SWALLOW",
                                severity="warn",
                                file=rel,
                                line=txt.count("\n", 0, m.start() + cm.start()) + 1,
                                evidence=f"suspend fun catches Exception/Throwable without rethrowing CancellationException",
                            )
                        )
                        break

            elif f in ("build.gradle", "build.gradle.kts"):
                # 5. DG-DEBUGSIGN: brace-scoped release { ... } signed with debug config
                for start_ln, block in _extract_gradle_block(txt, "release"):
                    if re.search(
                        r"signingConfig\s*=\s*signingConfigs\.(?:getByName\(\s*\"debug\"\s*\)|debug)",
                        block,
                    ):
                        findings.append(
                            Finding(
                                rule_id="DG-DEBUGSIGN",
                                severity="block",
                                file=rel,
                                line=start_ln,
                                evidence="release buildType uses signingConfigs.debug",
                            )
                        )
                for m in re.finditer(r"\"(https?://[^\"\s\\]+)\"", txt):
                    u = m.group(1)
                    if re.search(r"example\.com|example\.org|10\.0\.2\.2|localhost|127\.0\.0\.1", u, re.I):
                        findings.append(
                            Finding(
                                rule_id="DG-INVENTED-URL",
                                severity="warn",
                                file=rel,
                                line=txt.count("\n", 0, m.start()) + 1,
                                evidence=f"placeholder/dev-only URL in Gradle script: {u[:65]}",
                            )
                        )
                        break

            elif f.endswith(".pro"):
                for ln_idx, ln in enumerate(txt.splitlines(), 1):
                    if re.search(r"^\s*-keep\s+(?:public\s+)?class\s+(?:[\w.]+\.)?\*\*\s*\{\s*\*\s*;\s*\}", ln):
                        findings.append(
                            Finding(
                                rule_id="DG-BROADKEEP",
                                severity="warn",
                                file=rel,
                                line=ln_idx,
                                evidence=f"overbroad keep rule: {ln.strip()[:80]}",
                            )
                        )
                        break

    blocking = [f for f in findings if f.severity == "block"]
    warnings = [f for f in findings if f.severity == "warn"]
    verdict = "BLOCK" if blocking else ("WARN" if warnings else "PASS")
    return {
        "project": str(project_dir),
        "kt_files": kt_count,
        "verdict": verdict,
        "blocking_count": len(blocking),
        "warning_count": len(warnings),
        "rule_ids": sorted({f.rule_id for f in findings}),
        "findings": [asdict(f) for f in findings],
    }


def main() -> int:
    ap = argparse.ArgumentParser(description="Diff-Guard: monotonic security invariant & blind-spot gate")
    sub = ap.add_subparsers(dest="cmd", required=True)

    pd = sub.add_parser("diff", help="Check a unified diff patch against monotonic security invariants")
    pd.add_argument("patch_file", type=pathlib.Path)
    pd.add_argument("--json", action="store_true")
    pd.add_argument("--strict", action="store_true", help="Exit 1 on BLOCK verdict")

    ps = sub.add_parser("scan", help="Scan a project directory for slop-scan security/concurrency blind spots")
    ps.add_argument("project_dir", type=pathlib.Path)
    ps.add_argument("--json", action="store_true")
    ps.add_argument("--strict", action="store_true", help="Exit 1 on BLOCK verdict")

    args = ap.parse_args()
    if args.cmd == "diff":
        res = check_diff(args.patch_file.read_text(errors="replace"))
    else:
        res = scan_project(args.project_dir)

    if args.json:
        print(json.dumps(res, indent=2))
    else:
        print(f"{res['verdict']:5s} | blocking={res['blocking_count']} warnings={res['warning_count']}")
        for f in res["findings"]:
            print(f"  [{f['severity'].upper():5s} {f['rule_id']}] {f['file']}:{f['line']} — {f['evidence']}")
    return 1 if (args.strict and res["verdict"] == "BLOCK") else 0


if __name__ == "__main__":
    sys.exit(main())
