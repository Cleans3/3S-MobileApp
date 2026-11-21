# codex-luna-B2-detailed-functional-r2
bundle=codex-luna style=detailed-functional brief=B2 rep=2 files=9 kotlin_loc=91 build=fail

## files
- app/build.gradle.kts (23)
- app/src/main/AndroidManifest.xml (10)
- app/src/main/java/vn/shelftap/app/Api.kt (36)
- app/src/main/java/vn/shelftap/app/Data.kt (24)
- app/src/main/java/vn/shelftap/app/MainActivity.kt (39)
- app/src/main/res/values/styles.xml (1)
- build.gradle.kts (5)
- gradle.properties (4)
- settings.gradle.kts (11)

## manifest flags
- allowBackup="false"
- android.permission.INTERNET
- exported="true"

## descriptive counters (aid only)
- todo/fixme/stub: 0
- empty catch: 0
- !! operator: 3
- GlobalScope/runBlocking: 0
- log/println: 0
- hardcoded mock data: 0
- http:// literal: 0
- commented-out code: 0
- @Suppress: 0

## detector findings (rule: n)
- D02: 9
- P10: 2
- P11: 1
- S01: 3
- U04: 1
- detekt:MultipleEmitters: 1
- detekt:ViewModelForwarding: 6

## finding sites (first 3 per rule)
- D02 app/build.gradle.kts:12 androidx.compose:compose-bom:2024.10.01 behind latest stable 2026.11.01 (major_behind=2, gap_months=24.6)
- D02 app/build.gradle.kts:13 androidx.activity:activity-compose:1.9.3 behind latest stable 1.13.0 (major_behind=0, gap_months=16.8)
- D02 app/build.gradle.kts:17 androidx.lifecycle:lifecycle-runtime-compose:2.8.7 behind latest stable 2.11.1 (major_behind=0, gap_months=24.
- P10 app/build.gradle.kts:1 no release build type: R8 minification off by default
- P10 build.gradle.kts:1 no release build type: R8 minification off by default
- P11 app/build.gradle.kts:1 no baselineprofile plugin and no baseline-prof.txt
- S01 app/src/main/java/vn/shelftap/app/Data.kt:16 Preferences DataStore key "access_token" stored without encryption
- S01 app/src/main/java/vn/shelftap/app/Data.kt:17 Preferences DataStore key "refresh_token" stored without encryption
- S01 app/src/main/java/vn/shelftap/app/Data.kt:18 Preferences DataStore key "must_change_password" stored without encryption
- U04 app/src/main/java/vn/shelftap/app/MainActivity.kt:39 hardcoded Color(0xFF345D9D)),content=content) } bypasses the theme
- detekt:MultipleEmitters app/src/main/java/vn/shelftap/app/MainActivity.kt:33 Composable functions should only be emitting content into the composition from one source at their top level. 
- detekt:ViewModelForwarding app/src/main/java/vn/shelftap/app/MainActivity.kt:34 Forwarding a ViewModel/Presenter through multiple @Composable functions should be avoided. Consider using stat
- detekt:ViewModelForwarding app/src/main/java/vn/shelftap/app/MainActivity.kt:35 Forwarding a ViewModel/Presenter through multiple @Composable functions should be avoided. Consider using stat
- detekt:ViewModelForwarding app/src/main/java/vn/shelftap/app/MainActivity.kt:37 Forwarding a ViewModel/Presenter through multiple @Composable functions should be avoided. Consider using stat

## coder notes (fill in; copy codes to codes.csv)
