"""Security / privacy rules S01-S09 (S10, S12 cited-only; S11 is depcheck.py)."""
import re
import xml.etree.ElementTree as ET
from slopscan import rule, F
from ktlite import named_arg, split_top

ANS = "{http://schemas.android.com/apk/res/android}"
SENS = re.compile(r"token|jwt|passw|secret|refresh|credential|bearer|session|auth|email|member|history|loan|fullname|cccd|national", re.I)
SENS_FILE = re.compile(r"token|jwt|passw|secret|credential|bearer|refresh|cookie", re.I)   # file writes: only unambiguous credential words in the same statement
ENCRYPTED = re.compile(r"\bCipher\b|AndroidKeyStore|\bKeyStore\b|\bAead\b|Tink|\bEncryptedFile\b|SecretKey|KeyGenParameterSpec|encrypt\w*\(|CryptoManager|\bcrypto\b", re.I)

def attr(el, n): return el.get(ANS + n)

def parse(path, text):
    try: return ET.fromstring(text)
    except ET.ParseError: return None

def xml_line(text, needle):
    i = text.find(needle); return text[:i].count("\n") + 1 if i >= 0 else 1

@rule("S01")
def s01(p):
    """Token/credential/PII written to plain SharedPreferences, plain Preferences DataStore, a plain file, or a Room entity, with no Keystore/Cipher use in the same file."""
    out = []
    for k in p.kt:
        enc = bool(ENCRYPTED.search(k.skel)) or "EncryptedSharedPreferences" in k.skel   # encrypted store: not S01 (S08 flags the deprecated lib)
        for m in re.finditer(r"\.(putString|putStringSet|putInt|putLong|putBoolean)\s*\(\s*([^,)]+)\s*,\s*([^)]*)\)", k.code):
            if SENS.search(m.group(2)) or SENS.search(m.group(3)):
                if not enc and not re.search(r"encrypt|cipher|seal|encode", m.group(3), re.I):
                    out.append(F("S01", k.path, k.line(m.start()), f"{m.group(1)}({m.group(2).strip()[:30]}, ...) plaintext SharedPreferences"))
        for m in re.finditer(r"(string|int|long|boolean)PreferencesKey\s*\(\s*\"([^\"]*)\"", k.code):
            if SENS.search(m.group(2)) and not re.fullmatch(r"(?i)session|auth", m.group(2)) and not enc:
                out.append(F("S01", k.path, k.line(m.start()), f"Preferences DataStore key \"{m.group(2)}\" stored without encryption"))
        for m in re.finditer(r"(?:\bopenFileOutput|\bFileOutputStream|\.writeText|\.writeBytes)\s*\(", k.code):
            stmt = k.code[max(0, m.start() - 80):m.end() + 100].split(";")[0]
            if SENS_FILE.search(stmt) and not enc: out.append(F("S01", k.path, k.line(m.start()), f"plain file write of sensitive data: {k.snippet(m.start(), n=70)}"))
        for m in re.finditer(r"@Entity\b[^{]*?class\s+(\w+)\s*\(([^)]*)\)", k.code, re.S):
            cols = [x for x in re.findall(r"va[lr]\s+(\w+)\s*:", m.group(2)) if re.search(r"token|passw|secret|jwt", x, re.I)]
            if cols and not enc: out.append(F("S01", k.path, k.line(m.start()), f"Room entity {m.group(1)} stores {cols} in plaintext"))
    return out

PLACEHOLDER = re.compile(r"your|xxx|changeme|replace|example|<|todo|placeholder|dummy|\.\.\.", re.I)
SECRET_NAME = r"(api[_-]?key|apikey|secret|client[_-]?secret|auth[_-]?token|access[_-]?token|password|passwd|private[_-]?key|bearer|token)"
@rule("S02")
def s02(p):
    """Hardcoded secret: provider-key patterns, secret-named string constants with a non-placeholder high-entropy value, buildConfigField/signing passwords with literals."""
    out = []
    def looks_secret(v):
        return len(v) >= 12 and re.fullmatch(r"[A-Za-z0-9_\-+/=.:]+", v) and not PLACEHOLDER.search(v) and not re.fullmatch(r"[a-z_.\-]+", v) and not v.startswith(("http", "/", "Bearer ")) and re.search(r"\d", v) and re.search(r"[A-Za-z]", v)
    for k in p.kt:
        for m in re.finditer(r"AIza[0-9A-Za-z_\-]{35}|sk-[A-Za-z0-9]{20,}|AKIA[0-9A-Z]{16}|ghp_[A-Za-z0-9]{30,}|xox[bp]-[A-Za-z0-9-]{20,}", k.code):
            out.append(F("S02", k.path, k.line(m.start()), f"provider key literal {m.group(0)[:8]}..."))
        for m in re.finditer(r"\b\w*" + SECRET_NAME + r"\w*\s*(?::\s*String\s*)?=\s*\"([^\"\n$]{8,})\"", k.code, re.I):
            if looks_secret(m.group(2)): out.append(F("S02", k.path, k.line(m.start()), f"{k.snippet(m.start(), n=40)} literal secret"))
        for m in re.finditer(r"Credentials\.basic\s*\(\s*\"[^\"]+\"\s*,\s*\"[^\"]+\"", k.code):
            out.append(F("S02", k.path, k.line(m.start()), "hardcoded basic-auth credentials"))
    for path, t in p.gradle:
        for m in re.finditer(r"buildConfigField\s*\(?\s*\"String\"\s*,\s*\"(\w*" + SECRET_NAME + r"\w*)\"\s*,\s*\"\\\"([^\"\\]+)\\\"\"", t, re.I):
            if looks_secret(m.group(3)): out.append(F("S02", path, t[:m.start()].count("\n") + 1, f"BuildConfig {m.group(1)} literal secret"))
        for m in re.finditer(r"(?<![\"'(.\w])(storePassword|keyPassword)\s*=?\s*[\"']([^\"'\n$]+)[\"']", t):
            out.append(F("S02", path, t[:m.start()].count("\n") + 1, f"{m.group(1)} literal in build script"))
    for path, t in p.xml:
        for m in re.finditer(r"AIza[0-9A-Za-z_\-]{35}|<string[^>]*name=\"\w*" + SECRET_NAME + r"\w*\"[^>]*>([^<]{12,})<", t, re.I):
            v = m.group(m.lastindex) if m.lastindex else m.group(0)
            if looks_secret(v) or v.startswith("AIza"): out.append(F("S02", path, t[:m.start()].count("\n") + 1, f"resource secret {v[:8]}..."))
    return out

@rule("S03")
def s03(p):
    """usesCleartextTraffic=true, cleartextTrafficPermitted=true outside debug-overrides, or an http:// URL literal (excluding XML namespaces and loopback docs)."""
    out = []
    for path, t in p.manifests():
        m = re.search(r"usesCleartextTraffic\s*=\s*\"true\"", t)
        if m: out.append(F("S03", path, t[:m.start()].count("\n") + 1, "android:usesCleartextTraffic=true"))
    for path, t in p.xml:
        if "network-security-config" in t:
            body = re.sub(r"<debug-overrides.*?</debug-overrides>", "", t, flags=re.S)
            body = re.sub(r"<!--.*?-->", lambda c: "\n" * c.group(0).count("\n"), body, flags=re.S)       # a comment that forbids the setting is not the setting
            m = re.search(r"cleartextTrafficPermitted\s*=\s*\"true\"", body)
            if m: out.append(F("S03", path, t[:m.start()].count("\n") + 1, "cleartextTrafficPermitted=true"))
    for k in p.kt:
        for m in re.finditer(r"\"(http://[^\"\s]+)\"", k.code):
            if re.search(r"schemas\.android|w3\.org|apache\.org|xml\.org|xmlpull|\.example/docs", m.group(1)): continue
            out.append(F("S03", k.path, k.line(m.start()), f"cleartext URL literal {m.group(1)[:50]}"))
    return out

@rule("S04")
def s04(p):
    """TLS validation disabled: trust-all X509TrustManager, ALLOW_ALL/true HostnameVerifier, onReceivedSslError proceed(), custom sslSocketFactory with a trust-all manager."""
    out = []
    for k in p.kt:
        for m in re.finditer(r"X509TrustManager|X509ExtendedTrustManager", k.skel):
            ctx = k.code[m.start():m.start() + 700]
            if re.search(r"fun\s+checkServerTrusted\s*\([^)]*\)\s*(:\s*\w+\s*)?\{\s*\}|checkServerTrusted[^{]*\{\s*(//[^\n]*\s*)*\}|trustAll|TrustAll|arrayOf\(\)", ctx) or "checkServerTrusted" in ctx and "throw" not in ctx:
                out.append(F("S04", k.path, k.line(m.start()), "X509TrustManager that does not validate the chain")); break
        for m in re.finditer(r"hostnameVerifier\s*(\(|=)?\s*\{[^}]*->\s*true|HostnameVerifier\s*\{[^}]*->\s*true|ALLOW_ALL_HOSTNAME_VERIFIER|object\s*:\s*HostnameVerifier[^}]*return\s*true|NoopHostnameVerifier", k.code, re.S):
            out.append(F("S04", k.path, k.line(m.start()), "hostname verifier accepts every host"))
        for m in re.finditer(r"onReceivedSslError[\s\S]{0,300}?\.proceed\s*\(", k.skel):
            out.append(F("S04", k.path, k.line(m.start()), "WebView SslErrorHandler.proceed() ignores TLS errors"))
        if re.search(r"trustAllCerts|TrustAllCerts|insecureTrust|unsafeOkHttp|getUnsafeOkHttpClient|disableSSL", k.code, re.I):
            m = re.search(r"trustAllCerts|TrustAllCerts|insecureTrust|unsafeOkHttp|getUnsafeOkHttpClient|disableSSL", k.code, re.I)
            out.append(F("S04", k.path, k.line(m.start()), f"trust-all helper: {m.group(0)}"))
    seen = set(); res = []
    for f in out:
        key = (f["file"], f["line"], f["evidence"])
        if key not in seen: seen.add(key); res.append(f)
    return res

LOGSINK = r"\b(Log\.[dviwe]|Timber\.[dviwe]|println|Log\.wtf)\s*\("
@rule("S05")
def s05(p):
    """Sensitive logging: a log call whose arguments name a token/credential/Authorization/body/PII, or HttpLoggingInterceptor at BODY/HEADERS not guarded by BuildConfig.DEBUG."""
    out = []
    for k in p.kt:
        for m in re.finditer(LOGSINK, k.skel):
            o = m.end() - 1; c = k.match.get(o)
            if c is None: continue
            args = re.sub(r'"(?:[^"\\]|\\.)*"', lambda mm: " ".join(re.findall(r"\$\{[^}]*\}|\$\w+", mm.group(0))), k.code[o:c])   # keep identifiers and templates, drop literal prose
            hit = re.search(r"token|passw|secret|authorization|bearer|jwt|refresh|cookie|response\.body|\.body\(\)|requestBody|email|memberId|fullName|history", args, re.I)
            if hit: out.append(F("S05", k.path, k.line(m.start()), f"{m.group(1)} logs {hit.group(0)}: {k.snippet(m.start(), n=70)}"))
        for m in re.finditer(r"Level\.(BODY|HEADERS)\b", k.skel):
            ctx = k.code[max(0, m.start() - 160):m.end() + 100]
            if not re.search(r"BuildConfig\.DEBUG|isDebuggable|Level\.NONE|\bNONE\b", ctx):
                out.append(F("S05", k.path, k.line(m.start()), f"HttpLoggingInterceptor Level.{m.group(1)} not guarded for release"))
    return out

@rule("S06")
def s06(p):
    """allowBackup is true or absent (default true) and the application has no dataExtractionRules/fullBackupContent exclusion file."""
    out = []
    for path, t in p.manifests():
        root = parse(path, t)
        if root is None: continue
        app = root.find("application")
        if app is None: continue
        ab = attr(app, "allowBackup")
        if ab == "false": continue
        rules = attr(app, "dataExtractionRules") or attr(app, "fullBackupContent")
        if rules:
            name = rules.split("/")[-1]
            ok = any(x[0].endswith("/" + name + ".xml") and "<exclude" in x[1] for x in p.xml)
            if ok: continue
        out.append(F("S06", path, xml_line(t, "<application"), f"allowBackup={'true' if ab else 'absent (default true)'}, no backup exclusion rules"))
    return out

@rule("S07")
def s07(p):
    """Component exported=true (or implicitly exported by an intent-filter) with no android:permission and not the MAIN/LAUNCHER activity."""
    out = []
    for path, t in p.manifests():
        root = parse(path, t)
        if root is None: continue
        for comp in root.iter():
            if comp.tag not in ("activity", "activity-alias", "service", "receiver", "provider"): continue
            exp = attr(comp, "exported"); perm = attr(comp, "permission")
            filt = comp.findall("intent-filter")
            launcher = any(a.get(ANS + "name") == "android.intent.action.MAIN" for f in filt for a in f.findall("action"))
            acts = {a.get(ANS + "name", "").split(".")[-1] for f in filt for a in f.findall("action")}
            protected = {"BOOT_COMPLETED", "LOCKED_BOOT_COMPLETED", "MY_PACKAGE_REPLACED", "PACKAGE_REPLACED", "TIMEZONE_CHANGED", "APPWIDGET_UPDATE", "APPWIDGET_ENABLED", "APPWIDGET_DELETED", "APPWIDGET_DISABLED", "APPWIDGET_OPTIONS_CHANGED"}
            if perm or launcher or (comp.tag == "receiver" and acts and acts <= protected): continue
            if exp == "true" or (exp is None and filt):
                out.append(F("S07", path, xml_line(t, attr(comp, "name") or "<" + comp.tag), f"{comp.tag} {attr(comp, 'name')} exported without permission"))
    return out

@rule("S08")
def s08(p):
    """MD5/SHA-1 digest, AES default/ECB/DES/RC4, static or all-zero IV, hardcoded SecretKeySpec key, or androidx.security-crypto (EncryptedSharedPreferences/MasterKey/EncryptedFile)."""
    out = []
    for k in p.kt:
        for m in re.finditer(r"MessageDigest\.getInstance\(\s*\"(MD5|SHA-?1)\"", k.code, re.I): out.append(F("S08", k.path, k.line(m.start()), f"weak digest {m.group(1)}"))
        for m in re.finditer(r"Cipher\.getInstance\(\s*\"([^\"]*)\"", k.code):
            v = m.group(1).upper()
            if v.startswith("RSA"):
                if "NOPADDING" in v: out.append(F("S08", k.path, k.line(m.start()), f"textbook RSA transformation {m.group(1)}"))
            elif v in ("AES", "DES", "DESEDE", "RC4", "ARCFOUR") or "ECB" in v or v.startswith("DES"): out.append(F("S08", k.path, k.line(m.start()), f"weak cipher transformation {m.group(1)}"))
        for m in re.finditer(r"IvParameterSpec\s*\(\s*(ByteArray\s*\(\s*\d+\s*\)|\"[^\"]*\"\.toByteArray\([^)]*\)|byteArrayOf\([^)]*\)|[A-Z_]{3,})|GCMParameterSpec\s*\(\s*\d+\s*,\s*(ByteArray\s*\(\s*\d+\s*\)|\"[^\"]*\"\.toByteArray)", k.code):
            out.append(F("S08", k.path, k.line(m.start()), f"static/zero IV {k.snippet(m.start(), n=60)}"))
        for m in re.finditer(r"SecretKeySpec\s*\(\s*(\"[^\"]*\"\.toByteArray|byteArrayOf|[A-Z_]{4,}\b)", k.code):
            out.append(F("S08", k.path, k.line(m.start()), "hardcoded symmetric key"))
        for m in re.finditer(r"EncryptedSharedPreferences|\bMasterKeys?\b|androidx\.security\.crypto|EncryptedFile\b", k.code):
            out.append(F("S08", k.path, k.line(m.start()), f"deprecated androidx.security-crypto API {m.group(0)}")); break
    for path, t in p.gradle + p.toml:
        m = re.search(r"androidx\.security:security-crypto|security-crypto", t)
        if m: out.append(F("S08", path, t[:m.start()].count("\n") + 1, "dependency androidx.security:security-crypto (deprecated since 1.1.0-alpha07/1.1.0)"))
    return out

VALID = re.compile(r"isBlank|isEmpty|isNotBlank|isNotEmpty|Patterns\.EMAIL|\.matches\(|Regex\(|\.length|\.contains\(\"@\"|require\(|check\(|isValid|validate", re.I)
@rule("S09")
def s09(p):
    """SQL built by string interpolation/concatenation, WebView.loadUrl on a non-literal with JavaScript on, or a credential-submitting function with no validation predicate in its file."""
    out = []
    for k in p.kt:
        for m in re.finditer(r"\b(rawQuery|execSQL|SimpleSQLiteQuery)\s*\(\s*\"[^\"]*(\$\w|\$\{)[^\"]*\"|\b(rawQuery|execSQL)\s*\(\s*\"[^\"]*\"\s*\+", k.code):
            out.append(F("S09", k.path, k.line(m.start()), f"SQL built by interpolation: {k.snippet(m.start(), n=70)}"))
        if re.search(r"javaScriptEnabled\s*=\s*true|setJavaScriptEnabled\(\s*true", k.code):
            for m in re.finditer(r"\.loadUrl\s*\(\s*(?!\")([^)]+)\)", k.code):
                out.append(F("S09", k.path, k.line(m.start()), f"WebView loads non-literal URL {m.group(1)[:30]} with JavaScript enabled"))
        authish = re.search(r"(?i)(viewmodel|screen|activity|fragment)", k.path) and re.search(r"(?i)(sign|log)in|auth|regist|account|password", k.path)
        for f in k.funs:
            if authish and re.fullmatch(r"(login|signIn|sign_in|authenticate|changePassword|register|signUp)\w*", f.name, re.I) and re.search(r"(email|username|password|currentPassword|newPassword)\s*:\s*String", f.params) and not f.composable:
                body = k.code[f.bstart:f.bend]
                if not VALID.search(body) and not VALID.search(k.code):
                    out.append(F("S09", k.path, k.line(f.start), f"{f.name}(...) submits credentials; no validation predicate in {k.path}"))
    return out
