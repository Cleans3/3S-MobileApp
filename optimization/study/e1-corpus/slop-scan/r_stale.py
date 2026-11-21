"""D01: deprecated Android / Jetpack / Kotlin API use, from a curated table. Each entry carries the deprecating release and a source page.

The table is data, not logic: a wrong row is a precision bug, fixed in the open (protocol 11 amendments). Dependency age (D02) is depcheck.py.
"""
import re
from slopscan import rule, F

A = "https://developer.android.com/reference/"
TABLE = [  # (name, regex over code, replacement, source)
    ("AsyncTask", r"\bAsyncTask\b", "coroutines / Executors (deprecated API 30)", A + "android/os/AsyncTask"),
    ("onBackPressed", r"override\s+fun\s+onBackPressed\s*\(|\bsuper\.onBackPressed\s*\(|\bonBackPressed\s*\(\s*\)", "OnBackPressedDispatcher / BackHandler (deprecated API 33)", A + "android/app/Activity#onBackPressed()"),
    ("startActivityForResult", r"\bstartActivityForResult\s*\(|override\s+fun\s+onActivityResult\s*\(", "Activity Result APIs (AndroidX Activity 1.2)", A + "android/app/Activity#startActivityForResult(android.content.Intent,%20int)"),
    ("ProgressDialog", r"\bProgressDialog\b", "ProgressIndicator (deprecated API 26)", A + "android/app/ProgressDialog"),
    ("Handler()", r"\bHandler\s*\(\s*\)", "Handler(Looper) (deprecated API 30)", A + "android/os/Handler#Handler()"),
    ("Resources.getColor(int)", r"\bresources\.getColor\s*\(\s*[^,()]+\)|\bgetResources\(\)\.getColor\s*\(\s*[^,()]+\)", "ContextCompat.getColor (deprecated API 23)", A + "android/content/res/Resources#getColor(int)"),
    ("Resources.getDrawable(int)", r"\bresources\.getDrawable\s*\(\s*[^,()]+\)|\bgetResources\(\)\.getDrawable\s*\(\s*[^,()]+\)", "ResourcesCompat.getDrawable (deprecated API 22)", A + "android/content/res/Resources#getDrawable(int)"),
    ("Html.fromHtml(String)", r"\bHtml\.fromHtml\s*\(\s*[^,()]+\)", "Html.fromHtml(String, int) (deprecated API 24)", A + "android/text/Html#fromHtml(java.lang.String)"),
    ("NetworkInfo", r"\bNetworkInfo\b|\bgetActiveNetworkInfo\b|\bactiveNetworkInfo\b", "NetworkCapabilities (deprecated API 29)", A + "android/net/NetworkInfo"),
    ("Vibrator.vibrate(long)", r"\.vibrate\s*\(\s*\d+L?\s*\)", "VibrationEffect (deprecated API 26)", A + "android/os/Vibrator#vibrate(long)"),
    ("Window.statusBarColor", r"\.statusBarColor\s*=|\.navigationBarColor\s*=|\bsetStatusBarColor\s*\(", "edge-to-edge insets (deprecated API 35)", A + "android/view/Window#setStatusBarColor(int)"),
    ("Build.SERIAL", r"\bBuild\.SERIAL\b", "Build.getSerial (deprecated API 26)", A + "android/os/Build#SERIAL"),
    ("android.preference.PreferenceManager", r"import\s+android\.preference\.PreferenceManager", "androidx.preference (deprecated API 29)", A + "android/preference/PreferenceManager"),
    ("getExternalStorageDirectory", r"\bgetExternalStorageDirectory\s*\(|\bexternalStorageDirectory\b", "scoped storage (deprecated API 29)", A + "android/os/Environment#getExternalStorageDirectory()"),
    ("rememberRipple", r"\brememberRipple\s*\(", "ripple() (Compose Material 1.7)", "https://developer.android.com/jetpack/androidx/releases/compose-material#1.7.0"),
    ("Divider (M3)", r"(?<![\w.])Divider\s*\(", "HorizontalDivider (Material3 1.3.0)", "https://developer.android.com/jetpack/androidx/releases/compose-material3#1.3.0"),
    ("smallTopAppBarColors", r"\b(small|medium|large)TopAppBarColors\b", "topAppBarColors (Material3 1.2.0)", "https://developer.android.com/jetpack/androidx/releases/compose-material3#1.2.0"),
    ("non-AutoMirrored Icons", r"Icons\.(Filled|Outlined|Rounded|Sharp|TwoTone|Default)\.(ArrowBack|ArrowForward|ArrowLeft|ArrowRight|KeyboardArrowLeft|KeyboardArrowRight|ExitToApp|Send|List|Logout|Login|NavigateNext|NavigateBefore)\b", "Icons.AutoMirrored.* (material-icons 1.7)", "https://developer.android.com/jetpack/androidx/releases/compose-material#1.7.0"),
    ("animateItemPlacement", r"\banimateItemPlacement\s*\(", "animateItem (Foundation 1.7)", "https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.7.0"),
    ("ui.platform.LocalLifecycleOwner", r"androidx\.compose\.ui\.platform\.LocalLifecycleOwner", "androidx.lifecycle.compose.LocalLifecycleOwner (Compose UI 1.8)", "https://developer.android.com/jetpack/androidx/releases/compose-ui#1.8.0"),
    ("accompanist", r"com\.google\.accompanist\.(systemuicontroller|swiperefresh|pager|flowlayout|navigation\.animation|insets|placeholder)", "official Compose APIs (Accompanist deprecation notices)", "https://google.github.io/accompanist/"),
    ("kotlinx.android.synthetic", r"kotlinx\.android\.synthetic|kotlin-android-extensions", "View Binding (removed in Kotlin 1.8)", "https://developer.android.com/topic/libraries/view-binding/migration"),
]
GRADLE = [
    ("kotlinOptions", r"\bkotlinOptions\s*\{", "compilerOptions DSL (Kotlin Gradle plugin)", "https://kotlinlang.org/docs/gradle-compiler-options.html"),
    ("compileSdkVersion", r"\b(compileSdkVersion|targetSdkVersion|minSdkVersion)\s*[\s(]\s*\d+", "compileSdk/targetSdk/minSdk properties (AGP 8 DSL)", "https://developer.android.com/build/releases/past-releases/agp-8-0-0-release-notes"),
    ("buildToolsVersion", r"\bbuildToolsVersion\b", "AGP picks the build tools (AGP 8)", "https://developer.android.com/build/releases/gradle-plugin"),
    ("jcenter", r"\bjcenter\s*\(", "mavenCentral() (JCenter is sunset)", "https://developer.android.com/build/jcenter-migration"),
    ("accompanist-dep", r"com\.google\.accompanist:accompanist-(systemuicontroller|swiperefresh|pager|flowlayout|navigation-animation|insets|placeholder)", "official Compose APIs", "https://google.github.io/accompanist/"),
]

@rule("D01")
def d01(p):
    """Use of a deprecated API/DSL from TABLE/GRADLE (code, manifests and build scripts); security-crypto is also reported by S08 (taxonomy overlap is intended)."""
    out = []
    for k in p.kt:
        for name, rx, repl, src in TABLE:
            for m in re.finditer(rx, k.code):
                out.append(F("D01", k.path, k.line(m.start()), f"deprecated {name}: use {repl}", source=src))
        for m in re.finditer(r"EncryptedSharedPreferences|\bMasterKeys?\b|androidx\.security\.crypto", k.code):
            out.append(F("D01", k.path, k.line(m.start()), "deprecated androidx.security-crypto API (all APIs deprecated since 1.1.0)", source="https://developer.android.com/jetpack/androidx/releases/security")); break
    for path, t in p.gradle + p.toml:
        for name, rx, repl, src in GRADLE:
            for m in re.finditer(rx, t):
                out.append(F("D01", path, t[:m.start()].count("\n") + 1, f"deprecated {name}: use {repl}", source=src))
        m = re.search(r"security-crypto", t)
        if m: out.append(F("D01", path, t[:m.start()].count("\n") + 1, "dependency androidx.security:security-crypto (deprecated)", source="https://developer.android.com/jetpack/androidx/releases/security"))
    return out
