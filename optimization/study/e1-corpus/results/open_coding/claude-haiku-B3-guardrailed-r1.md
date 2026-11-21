# claude-haiku-B3-guardrailed-r1
bundle=claude-haiku style=guardrailed brief=B3 rep=1 files=36 kotlin_loc=1281 build=fail

## files
- .gitignore (11)
- README.md (40)
- app/build.gradle.kts (68)
- app/proguard-rules.pro (15)
- app/src/main/AndroidManifest.xml (26)
- app/src/main/java/vn/shelftap/app/AppContainer.kt (42)
- app/src/main/java/vn/shelftap/app/MainActivity.kt (18)
- app/src/main/java/vn/shelftap/app/ShelfTapApplication.kt (11)
- app/src/main/java/vn/shelftap/app/api/Models.kt (51)
- app/src/main/java/vn/shelftap/app/api/ShelfTapApi.kt (92)
- app/src/main/java/vn/shelftap/app/auth/AuthRepository.kt (16)
- app/src/main/java/vn/shelftap/app/data/LibraryRepository.kt (79)
- app/src/main/java/vn/shelftap/app/data/SecureCache.kt (95)
- app/src/main/java/vn/shelftap/app/ui/ShelfTapRoot.kt (127)
- app/src/main/java/vn/shelftap/app/ui/common/Components.kt (55)
- app/src/main/java/vn/shelftap/app/ui/history/HistoryScreen.kt (188)
- app/src/main/java/vn/shelftap/app/ui/history/HistoryViewModel.kt (133)
- app/src/main/java/vn/shelftap/app/ui/loans/LoansScreen.kt (177)
- app/src/main/java/vn/shelftap/app/ui/loans/LoansViewModel.kt (127)
- app/src/main/java/vn/shelftap/app/ui/theme/Theme.kt (13)
- app/src/main/java/vn/shelftap/app/ui/title/TitleScreen.kt (132)
- app/src/main/java/vn/shelftap/app/ui/title/TitleViewModel.kt (45)
- app/src/main/res/drawable/ic_launcher_foreground.xml (10)
- app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml (5)
- app/src/main/res/values/colors.xml (4)
- app/src/main/res/values/plurals.xml (7)
- app/src/main/res/values/strings.xml (40)
- app/src/main/res/values/themes.xml (4)
- app/src/main/res/xml/network_security_config.xml (8)
- baselineprofile/build.gradle.kts (42)
- baselineprofile/src/main/AndroidManifest.xml (2)
- baselineprofile/src/main/java/vn/shelftap/app/baselineprofile/BaselineProfileGenerator.kt (26)
- build.gradle.kts (8)
- gradle/libs.versions.toml (46)
- gradle.properties (5)
- settings.gradle.kts (18)

## manifest flags
- allowBackup="false"
- android.permission.INTERNET
- exported="true"

## descriptive counters (aid only)
- todo/fixme/stub: 0
- empty catch: 0
- !! operator: 0
- GlobalScope/runBlocking: 0
- log/println: 0
- hardcoded mock data: 0
- http:// literal: 0
- commented-out code: 0
- @Suppress: 0

## detector findings (rule: n)
- D02: 18
- P07: 1
- P10: 1
- U05: 1
- detekt:ModifierMissing: 4
- detekt:ModifierWithoutDefault: 1

## finding sites (first 3 per rule)
- D02 gradle/libs.versions.toml:21 androidx.core:core-ktx:1.15.0 behind latest stable 1.19.1 (major_behind=0, gap_months=22.8)
- D02 gradle/libs.versions.toml:22 androidx.activity:activity-compose:1.9.3 behind latest stable 1.13.0 (major_behind=0, gap_months=16.8)
- D02 gradle/libs.versions.toml:23 androidx.lifecycle:lifecycle-runtime-compose:2.8.7 behind latest stable 2.11.1 (major_behind=0, gap_months=24.
- P07 app/src/main/java/vn/shelftap/app/api/ShelfTapApi.kt:69 newCall(request).execute().use { response -> val text = response.body?.string( not under an IO dispatcher/thre
- P10 build.gradle.kts:1 no release build type: R8 minification off by default
- U05 app/src/main/java/vn/shelftap/app/ui/title/TitleScreen.kt:1 data screen file renders no loading state
- detekt:ModifierMissing app/src/main/java/vn/shelftap/app/ui/ShelfTapRoot.kt:45 This @Composable function emits content but doesn't have a modifier parameter. See https://mrmans0n.github.io/
- detekt:ModifierMissing app/src/main/java/vn/shelftap/app/ui/history/HistoryScreen.kt:43 This @Composable function emits content but doesn't have a modifier parameter. See https://mrmans0n.github.io/
- detekt:ModifierMissing app/src/main/java/vn/shelftap/app/ui/loans/LoansScreen.kt:47 This @Composable function emits content but doesn't have a modifier parameter. See https://mrmans0n.github.io/
- detekt:ModifierWithoutDefault app/src/main/java/vn/shelftap/app/ui/title/TitleScreen.kt:74 This @Composable function has a modifier parameter but it doesn't have a default value. See https://mrmans0n.g

## coder notes (fill in; copy codes to codes.csv)
