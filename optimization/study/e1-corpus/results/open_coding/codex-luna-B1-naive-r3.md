# codex-luna-B1-naive-r3
bundle=codex-luna style=naive brief=B1 rep=3 files=9 kotlin_loc=140 build=fail

## files
- app/build.gradle.kts (19)
- app/src/main/AndroidManifest.xml (17)
- app/src/main/kotlin/com/example/librarycatalogue/MainActivity.kt (153)
- app/src/main/res/values/colors.xml (3)
- app/src/main/res/values/strings.xml (3)
- app/src/main/res/values/styles.xml (10)
- build.gradle.kts (4)
- gradle.properties (3)
- settings.gradle.kts (20)

## manifest flags
- allowBackup="false"
- exported="true"

## descriptive counters (aid only)
- todo/fixme/stub: 0
- empty catch: 0
- !! operator: 0
- GlobalScope/runBlocking: 0
- log/println: 0
- hardcoded mock data: 1
- http:// literal: 0
- commented-out code: 0
- @Suppress: 0

## detector findings (rule: n)
- D01: 1
- D02: 2
- P10: 2
- P11: 1
- P13: 1

## finding sites (first 3 per rule)
- D01 app/src/main/kotlin/com/example/librarycatalogue/MainActivity.kt:23 deprecated Window.statusBarColor: use edge-to-edge insets (deprecated API 35)
- D02 build.gradle.kts:2 com.android.application:com.android.application.gradle.plugin:8.5.2 behind latest stable 9.5.2 (major_behind=1
- D02 build.gradle.kts:3 org.jetbrains.kotlin.android:org.jetbrains.kotlin.android.gradle.plugin:1.9.24 behind latest stable 2.4.21 (ma
- P10 app/build.gradle.kts:1 no release build type: R8 minification off by default
- P10 build.gradle.kts:1 no release build type: R8 minification off by default
- P11 app/build.gradle.kts:1 no baselineprofile plugin and no baseline-prof.txt
- P13 app/src/main/kotlin/com/example/librarycatalogue/MainActivity.kt:103 linear filter over the list on the search query, no index; debounce=no

## coder notes (fill in; copy codes to codes.csv)
