# codex-luna-B3-guardrailed-r1
bundle=codex-luna style=guardrailed brief=B3 rep=1 files=16 kotlin_loc=201 build=fail

## files
- app/build.gradle.kts (42)
- app/proguard-rules.pro (1)
- app/src/main/AndroidManifest.xml (11)
- app/src/main/baseline-prof.txt (3)
- app/src/main/java/vn/shelftap/app/MainActivity.kt (88)
- app/src/main/java/vn/shelftap/app/data/Api.kt (53)
- app/src/main/java/vn/shelftap/app/data/EncryptedCache.kt (36)
- app/src/main/java/vn/shelftap/app/data/Models.kt (6)
- app/src/main/java/vn/shelftap/app/ui/ShelfTapViewModel.kt (43)
- app/src/main/res/values/colors.xml (1)
- app/src/main/res/values/strings.xml (5)
- app/src/main/res/values/themes.xml (7)
- build.gradle.kts (5)
- gradle/libs.versions.toml (28)
- gradle.properties (3)
- settings.gradle.kts (9)

## manifest flags
- allowBackup="false"
- android.permission.INTERNET
- exported="true"

## descriptive counters (aid only)
- todo/fixme/stub: 0
- empty catch: 0
- !! operator: 1
- GlobalScope/runBlocking: 0
- log/println: 0
- hardcoded mock data: 0
- http:// literal: 0
- commented-out code: 0
- @Suppress: 0

## detector findings (rule: n)
- D01: 1
- D02: 7
- P02: 1
- P03: 1
- P07: 1
- P10: 1
- U04: 2

## finding sites (first 3 per rule)
- D01 app/build.gradle.kts:26 deprecated kotlinOptions: use compilerOptions DSL (Kotlin Gradle plugin)
- D02 gradle/libs.versions.toml:13 androidx.core:core-ktx:1.15.0 behind latest stable 1.19.1 (major_behind=0, gap_months=22.8)
- D02 gradle/libs.versions.toml:14 androidx.activity:activity-compose:1.10.0 behind latest stable 1.13.0 (major_behind=0, gap_months=13.8)
- D02 gradle/libs.versions.toml:15 androidx.lifecycle:lifecycle-runtime-compose:2.8.7 behind latest stable 2.11.1 (major_behind=0, gap_months=24.
- P02 app/src/main/java/vn/shelftap/app/MainActivity.kt:80 LazyColumn with 5 content blocks, no contentType
- P03 app/src/main/java/vn/shelftap/app/MainActivity.kt:54 fun LoansScreen(loans: List<Loan>) unstable parameter
- P07 app/src/main/java/vn/shelftap/app/data/Api.kt:32 URL(baseUrl + path).openConnection() as HttpURLConnection).apply { requestMeth not under an IO dispatcher/thre
- P10 build.gradle.kts:1 no release build type: R8 minification off by default
- U04 app/src/main/java/vn/shelftap/app/MainActivity.kt:35 hardcoded Color(0xFF245B4A), secondary = androidx. bypasses the theme
- U04 app/src/main/java/vn/shelftap/app/MainActivity.kt:35 hardcoded Color(0xFF526B5D)), content = content) } bypasses the theme

## coder notes (fill in; copy codes to codes.csv)
