# codex-luna-B1-guardrailed-r2
bundle=codex-luna style=guardrailed brief=B1 rep=2 files=18 kotlin_loc=338 build=fail

## files
- app/build.gradle.kts (50)
- app/proguard-rules.pro (1)
- app/src/main/AndroidManifest.xml (11)
- app/src/main/java/vn/shelftap/app/MainActivity.kt (28)
- app/src/main/java/vn/shelftap/app/data/CatalogueApi.kt (20)
- app/src/main/java/vn/shelftap/app/data/CatalogueDao.kt (26)
- app/src/main/java/vn/shelftap/app/data/CatalogueDatabase.kt (21)
- app/src/main/java/vn/shelftap/app/data/CatalogueRepository.kt (44)
- app/src/main/java/vn/shelftap/app/data/Converters.kt (8)
- app/src/main/java/vn/shelftap/app/data/Title.kt (34)
- app/src/main/java/vn/shelftap/app/ui/ShelfTapApp.kt (133)
- app/src/main/java/vn/shelftap/app/ui/ShelfTapViewModel.kt (68)
- app/src/main/res/values/strings.xml (14)
- app/src/main/res/values/themes.xml (9)
- build.gradle.kts (6)
- gradle/libs.versions.toml (42)
- gradle.properties (4)
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
- @Suppress: 1

## detector findings (rule: n)
- D01: 1
- D02: 15
- P02: 1
- P06: 1
- P10: 1
- P11: 1
- S11: 1
- U05: 2
- detekt:LambdaParameterEventTrailing: 1
- detekt:ModifierMissing: 1
- detekt:ModifierWithoutDefault: 2
- detekt:ViewModelInjection: 1

## finding sites (first 3 per rule)
- D01 app/build.gradle.kts:25 deprecated kotlinOptions: use compilerOptions DSL (Kotlin Gradle plugin)
- D02 gradle/libs.versions.toml:17 androidx.core:core-ktx:1.15.0 behind latest stable 1.19.1 (major_behind=0, gap_months=22.8)
- D02 gradle/libs.versions.toml:18 androidx.activity:activity-compose:1.9.3 behind latest stable 1.13.0 (major_behind=0, gap_months=16.8)
- D02 gradle/libs.versions.toml:19 androidx.lifecycle:lifecycle-runtime-compose:2.8.7 behind latest stable 2.11.1 (major_behind=0, gap_months=24.
- P02 app/src/main/java/vn/shelftap/app/ui/ShelfTapApp.kt:114 LazyColumn with 7 content blocks, no contentType
- P06 app/src/main/java/vn/shelftap/app/ui/ShelfTapApp.kt:131 AsyncImage without a size-bounding modifier (decodes at original size)
- P10 build.gradle.kts:1 no release build type: R8 minification off by default
- P11 app/build.gradle.kts:1 no baselineprofile plugin and no baseline-prof.txt
- S11 gradle/libs.versions.toml:42 hallucinated-version com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.0.21-1.0.28 (repos: googl
- U05 app/src/main/java/vn/shelftap/app/ui/ShelfTapApp.kt:1 data screen file renders no loading state
- U05 app/src/main/java/vn/shelftap/app/ui/ShelfTapApp.kt:1 data screen file renders no empty state
- detekt:LambdaParameterEventTrailing app/src/main/java/vn/shelftap/app/ui/ShelfTapApp.kt:88 Lambda parameters in a @Composable that are for events (e.g. onClick, onChange, etc) and are required (they do
- detekt:ModifierMissing app/src/main/java/vn/shelftap/app/ui/ShelfTapApp.kt:53 This @Composable function emits content but doesn't have a modifier parameter. See https://mrmans0n.github.io/
- detekt:ModifierWithoutDefault app/src/main/java/vn/shelftap/app/ui/ShelfTapApp.kt:88 This @Composable function has a modifier parameter but it doesn't have a default value. See https://mrmans0n.g
- detekt:ModifierWithoutDefault app/src/main/java/vn/shelftap/app/ui/ShelfTapApp.kt:130 This @Composable function has a modifier parameter but it doesn't have a default value. See https://mrmans0n.g
- detekt:ViewModelInjection app/src/main/java/vn/shelftap/app/ui/ShelfTapApp.kt:54 Implicit dependencies of composables should be made explicit. Usages of viewModel to acquire a ViewModel shoul

## coder notes (fill in; copy codes to codes.csv)
