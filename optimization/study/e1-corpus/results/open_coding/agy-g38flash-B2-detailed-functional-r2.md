# agy-g38flash-B2-detailed-functional-r2
bundle=agy-g38flash style=detailed-functional brief=B2 rep=2 files=39 kotlin_loc=2109 build=fail

## files
- app/build.gradle.kts (93)
- app/proguard-rules.pro (5)
- app/src/main/AndroidManifest.xml (27)
- app/src/main/java/vn/shelftap/app/MainActivity.kt (28)
- app/src/main/java/vn/shelftap/app/ShelfTapApp.kt (71)
- app/src/main/java/vn/shelftap/app/data/local/SessionManager.kt (122)
- app/src/main/java/vn/shelftap/app/data/model/AuthModels.kt (117)
- app/src/main/java/vn/shelftap/app/data/remote/ApiException.kt (76)
- app/src/main/java/vn/shelftap/app/data/remote/AuthInterceptor.kt (120)
- app/src/main/java/vn/shelftap/app/data/remote/ErrorCodes.kt (12)
- app/src/main/java/vn/shelftap/app/data/remote/ResponseExtensions.kt (31)
- app/src/main/java/vn/shelftap/app/data/remote/ShelfTapApiService.kt (30)
- app/src/main/java/vn/shelftap/app/data/repository/AuthRepository.kt (51)
- app/src/main/java/vn/shelftap/app/data/repository/MemberRepository.kt (29)
- app/src/main/java/vn/shelftap/app/ui/navigation/AppNavigation.kt (156)
- app/src/main/java/vn/shelftap/app/ui/screens/password/ChangePasswordScreen.kt (323)
- app/src/main/java/vn/shelftap/app/ui/screens/password/ChangePasswordViewModel.kt (146)
- app/src/main/java/vn/shelftap/app/ui/screens/profile/ProfileScreen.kt (342)
- app/src/main/java/vn/shelftap/app/ui/screens/profile/ProfileViewModel.kt (120)
- app/src/main/java/vn/shelftap/app/ui/screens/signin/SignInScreen.kt (235)
- app/src/main/java/vn/shelftap/app/ui/screens/signin/SignInViewModel.kt (126)
- app/src/main/java/vn/shelftap/app/ui/theme/Color.kt (28)
- app/src/main/java/vn/shelftap/app/ui/theme/Theme.kt (80)
- app/src/main/java/vn/shelftap/app/ui/theme/Type.kt (59)
- app/src/main/res/drawable/ic_launcher_background.xml (10)
- app/src/main/res/drawable/ic_launcher_foreground.xml (14)
- app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml (5)
- app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml (5)
- app/src/main/res/values/colors.xml (16)
- app/src/main/res/values/strings.xml (20)
- app/src/main/res/values/themes.xml (6)
- app/src/test/java/vn/shelftap/app/data/remote/ApiExceptionTest.kt (113)
- app/src/test/java/vn/shelftap/app/ui/screens/password/PasswordValidationTest.kt (28)
- build.gradle.kts (4)
- gradle/wrapper/gradle-wrapper.properties (5)
- gradle.properties (4)
- gradlew (60)
- gradlew.bat (73)
- settings.gradle.kts (17)

## manifest flags
- allowBackup="true"
- android.permission.ACCESS_NETWORK_STATE
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
- @Suppress: 5

## detector findings (rule: n)
- D01: 2
- D02: 16
- P07: 1
- P10: 2
- P11: 1
- S01: 4
- S05: 2
- S06: 1
- U03: 2
- U04: 4
- detekt:ComposableParamOrder: 1
- detekt:LambdaParameterInRestartableEffect: 6
- detekt:ViewModelInjection: 1

## finding sites (first 3 per rule)
- D01 app/build.gradle.kts:38 deprecated kotlinOptions: use compilerOptions DSL (Kotlin Gradle plugin)
- D01 app/src/main/java/vn/shelftap/app/ui/theme/Theme.kt:70 deprecated Window.statusBarColor: use edge-to-edge insets (deprecated API 35)
- D02 app/build.gradle.kts:62 androidx.core:core-ktx:1.13.1 behind latest stable 1.19.1 (major_behind=0, gap_months=28.7)
- D02 app/build.gradle.kts:63 androidx.lifecycle:lifecycle-runtime-ktx:2.7.0 behind latest stable 2.11.1 (major_behind=0, gap_months=34.3)
- D02 app/build.gradle.kts:64 androidx.lifecycle:lifecycle-runtime-compose:2.7.0 behind latest stable 2.11.1 (major_behind=0, gap_months=34.
- P07 app/src/main/java/vn/shelftap/app/data/remote/AuthInterceptor.kt:102 newCall(request).execute() if (refreshResponse.isSuccessful) { not under an IO dispatcher/thread
- P10 app/build.gradle.kts:24 release build type has no isMinifyEnabled = true (R8 off by default)
- P10 build.gradle.kts:1 no release build type: R8 minification off by default
- P11 app/build.gradle.kts:1 no baselineprofile plugin and no baseline-prof.txt
- S01 app/src/main/java/vn/shelftap/app/data/local/SessionManager.kt:78 putString(KEY_ACCESS_TOKEN, ...) plaintext SharedPreferences
- S01 app/src/main/java/vn/shelftap/app/data/local/SessionManager.kt:79 putString(KEY_REFRESH_TOKEN, ...) plaintext SharedPreferences
- S01 app/src/main/java/vn/shelftap/app/data/local/SessionManager.kt:81 putBoolean(KEY_MUST_CHANGE_PASSWORD, ...) plaintext SharedPreferences
- S05 app/src/main/java/vn/shelftap/app/ShelfTapApp.kt:27 HttpLoggingInterceptor Level.BODY not guarded for release
- S05 app/src/main/java/vn/shelftap/app/ShelfTapApp.kt:43 HttpLoggingInterceptor Level.BODY not guarded for release
- S06 app/src/main/AndroidManifest.xml:7 allowBackup=true, no backup exclusion rules
- U03 app/src/main/java/vn/shelftap/app/ui/theme/Theme.kt:14 lightColorScheme: onSecondary on secondary contrast 3.74:1 < 4.5
- U03 app/src/main/java/vn/shelftap/app/ui/theme/Theme.kt:37 darkColorScheme: onSecondaryContainer on secondaryContainer contrast 3.74:1 < 4.5
- U04 app/src/main/java/vn/shelftap/app/ui/screens/profile/ProfileScreen.kt:322 hardcoded Color(0xFFDCFCE7) else Color(0xFFFEE2E2) bypasses the theme
- U04 app/src/main/java/vn/shelftap/app/ui/screens/profile/ProfileScreen.kt:322 hardcoded Color(0xFFFEE2E2) ) { bypasses the theme
- U04 app/src/main/java/vn/shelftap/app/ui/screens/profile/ProfileScreen.kt:328 hardcoded Color(0xFF166534) else Color(0xFF991B1B) bypasses the theme
- detekt:ComposableParamOrder app/src/main/java/vn/shelftap/app/ui/screens/profile/ProfileScreen.kt:293 Parameters in a composable function should be ordered following this pattern: params without defaults, modifie
- detekt:LambdaParameterInRestartableEffect app/src/main/java/vn/shelftap/app/ui/screens/password/ChangePasswordScreen.kt:60 Lambda parameters in a @Composable that are referenced directly inside of restarting effects can cause issues 
- detekt:LambdaParameterInRestartableEffect app/src/main/java/vn/shelftap/app/ui/screens/password/ChangePasswordScreen.kt:62 Lambda parameters in a @Composable that are referenced directly inside of restarting effects can cause issues 
- detekt:LambdaParameterInRestartableEffect app/src/main/java/vn/shelftap/app/ui/screens/profile/ProfileScreen.kt:57 Lambda parameters in a @Composable that are referenced directly inside of restarting effects can cause issues 
- detekt:ViewModelInjection app/src/main/java/vn/shelftap/app/ui/navigation/AppNavigation.kt:60 Implicit dependencies of composables should be made explicit. Usages of viewModel to acquire a ViewModel shoul

## coder notes (fill in; copy codes to codes.csv)
