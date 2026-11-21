# claude-haiku-B1-guardrailed-r3
bundle=claude-haiku style=guardrailed brief=B1 rep=3 files=35 kotlin_loc=985 build=fail

## files
- .gitignore (10)
- README.md (63)
- app/build.gradle.kts (67)
- app/proguard-rules.pro (4)
- app/src/main/AndroidManifest.xml (28)
- app/src/main/baseline-prof.txt (13)
- app/src/main/java/vn/shelftap/app/AppContainer.kt (39)
- app/src/main/java/vn/shelftap/app/MainActivity.kt (22)
- app/src/main/java/vn/shelftap/app/ShelfTapApplication.kt (16)
- app/src/main/java/vn/shelftap/app/data/CatalogueSync.kt (75)
- app/src/main/java/vn/shelftap/app/data/TitleMapping.kt (42)
- app/src/main/java/vn/shelftap/app/data/TitleRepository.kt (62)
- app/src/main/java/vn/shelftap/app/data/local/AppDatabase.kt (15)
- app/src/main/java/vn/shelftap/app/data/local/Daos.kt (45)
- app/src/main/java/vn/shelftap/app/data/local/Entities.kt (40)
- app/src/main/java/vn/shelftap/app/data/remote/CatalogueApi.kt (88)
- app/src/main/java/vn/shelftap/app/model/Title.kt (15)
- app/src/main/java/vn/shelftap/app/ui/CatalogueScreen.kt (36)
- app/src/main/java/vn/shelftap/app/ui/Components.kt (201)
- app/src/main/java/vn/shelftap/app/ui/DetailScreen.kt (134)
- app/src/main/java/vn/shelftap/app/ui/SearchScreen.kt (65)
- app/src/main/java/vn/shelftap/app/ui/ShelfTapRoot.kt (118)
- app/src/main/java/vn/shelftap/app/ui/ViewModels.kt (82)
- app/src/main/java/vn/shelftap/app/ui/theme/Theme.kt (24)
- app/src/main/res/drawable/ic_launcher_foreground.xml (10)
- app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml (5)
- app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml (5)
- app/src/main/res/values/colors.xml (4)
- app/src/main/res/values/strings.xml (31)
- app/src/main/res/values/themes.xml (5)
- app/src/main/res/xml/network_security_config.xml (9)
- build.gradle.kts (6)
- gradle/libs.versions.toml (42)
- gradle.properties (4)
- settings.gradle.kts (17)

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
- @Suppress: 1

## detector findings (rule: n)
- D01: 1
- D02: 16
- P07: 1
- P10: 1
- S11: 1
- U05: 3
- detekt:ModifierMissing: 7

## finding sites (first 3 per rule)
- D01 app/src/main/java/vn/shelftap/app/ui/ShelfTapRoot.kt:41 deprecated non-AutoMirrored Icons: use Icons.AutoMirrored.* (material-icons 1.7)
- D02 gradle/libs.versions.toml:17 androidx.core:core-ktx:1.15.0 behind latest stable 1.19.1 (major_behind=0, gap_months=22.8)
- D02 gradle/libs.versions.toml:18 androidx.activity:activity-compose:1.9.3 behind latest stable 1.13.0 (major_behind=0, gap_months=16.8)
- D02 gradle/libs.versions.toml:19 androidx.lifecycle:lifecycle-runtime-compose:2.8.7 behind latest stable 2.11.1 (major_behind=0, gap_months=24.
- P07 app/src/main/java/vn/shelftap/app/data/remote/CatalogueApi.kt:63 newCall(request).execute().use { response -> if (response.code == 404) return not under an IO dispatcher/threa
- P10 build.gradle.kts:1 no release build type: R8 minification off by default
- S11 gradle/libs.versions.toml:42 hallucinated-version com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.0.21-1.0.28 (repos: googl
- U05 app/src/main/java/vn/shelftap/app/ui/CatalogueScreen.kt:1 data screen file renders no loading state
- U05 app/src/main/java/vn/shelftap/app/ui/DetailScreen.kt:1 data screen file renders no error state
- U05 app/src/main/java/vn/shelftap/app/ui/SearchScreen.kt:1 data screen file renders no loading state
- detekt:ModifierMissing app/src/main/java/vn/shelftap/app/ui/CatalogueScreen.kt:19 This @Composable function emits content but doesn't have a modifier parameter. See https://mrmans0n.github.io/
- detekt:ModifierMissing app/src/main/java/vn/shelftap/app/ui/Components.kt:83 This @Composable function emits content but doesn't have a modifier parameter. See https://mrmans0n.github.io/
- detekt:ModifierMissing app/src/main/java/vn/shelftap/app/ui/Components.kt:142 This @Composable function emits content but doesn't have a modifier parameter. See https://mrmans0n.github.io/

## coder notes (fill in; copy codes to codes.csv)
