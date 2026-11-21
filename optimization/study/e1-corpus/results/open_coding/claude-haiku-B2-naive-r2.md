# claude-haiku-B2-naive-r2
bundle=claude-haiku style=naive brief=B2 rep=2 files=24 kotlin_loc=470 build=fail

## files
- .gitignore (10)
- README.md (51)
- app/build.gradle.kts (71)
- app/proguard-rules.pro (3)
- app/src/main/AndroidManifest.xml (25)
- app/src/main/java/com/example/library/LibraryApplication.kt (40)
- app/src/main/java/com/example/library/MainActivity.kt (53)
- app/src/main/java/com/example/library/data/AccountRepository.kt (39)
- app/src/main/java/com/example/library/data/AuthInterceptor.kt (23)
- app/src/main/java/com/example/library/data/LibraryApi.kt (18)
- app/src/main/java/com/example/library/data/Models.kt (21)
- app/src/main/java/com/example/library/data/SessionStore.kt (41)
- app/src/main/java/com/example/library/ui/AccountViewModel.kt (97)
- app/src/main/java/com/example/library/ui/LoginScreen.kt (102)
- app/src/main/java/com/example/library/ui/ProfileScreen.kt (87)
- app/src/main/java/com/example/library/ui/theme/Theme.kt (30)
- app/src/main/res/drawable/ic_launcher_foreground.xml (10)
- app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml (5)
- app/src/main/res/values/colors.xml (4)
- app/src/main/res/values/strings.xml (4)
- app/src/main/res/values/themes.xml (5)
- build.gradle.kts (5)
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
- @Suppress: 1

## detector findings (rule: n)
- D01: 3
- D02: 10
- P10: 1
- P11: 1
- S08: 2

## finding sites (first 3 per rule)
- D01 app/build.gradle.kts:38 deprecated kotlinOptions: use compilerOptions DSL (Kotlin Gradle plugin)
- D01 app/build.gradle.kts:63 dependency androidx.security:security-crypto (deprecated)
- D01 app/src/main/java/com/example/library/data/SessionStore.kt:6 deprecated androidx.security-crypto API (all APIs deprecated since 1.1.0)
- D02 app/build.gradle.kts:53 androidx.core:core-ktx:1.15.0 behind latest stable 1.19.1 (major_behind=0, gap_months=22.8)
- D02 app/build.gradle.kts:54 androidx.activity:activity-compose:1.9.3 behind latest stable 1.13.0 (major_behind=0, gap_months=16.8)
- D02 app/build.gradle.kts:58 androidx.lifecycle:lifecycle-runtime-ktx:2.8.7 behind latest stable 2.11.1 (major_behind=0, gap_months=24.6)
- P10 build.gradle.kts:1 no release build type: R8 minification off by default
- P11 app/build.gradle.kts:1 no baselineprofile plugin and no baseline-prof.txt
- S08 app/build.gradle.kts:63 dependency androidx.security:security-crypto (deprecated since 1.1.0-alpha07/1.1.0)
- S08 app/src/main/java/com/example/library/data/SessionStore.kt:6 deprecated androidx.security-crypto API androidx.security.crypto

## coder notes (fill in; copy codes to codes.csv)
