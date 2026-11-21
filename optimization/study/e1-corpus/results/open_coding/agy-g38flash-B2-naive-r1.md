# agy-g38flash-B2-naive-r1
bundle=agy-g38flash style=naive brief=B2 rep=1 files=44 kotlin_loc=2037 build=pass

## files
- app/build.gradle.kts (92)
- app/proguard-rules.pro (8)
- app/src/main/AndroidManifest.xml (29)
- app/src/main/java/com/example/libraryapp/LibraryApplication.kt (12)
- app/src/main/java/com/example/libraryapp/MainActivity.kt (38)
- app/src/main/java/com/example/libraryapp/data/local/SessionManager.kt (75)
- app/src/main/java/com/example/libraryapp/data/model/ApiResult.kt (7)
- app/src/main/java/com/example/libraryapp/data/model/AuthModels.kt (28)
- app/src/main/java/com/example/libraryapp/data/model/MemberModels.kt (65)
- app/src/main/java/com/example/libraryapp/data/remote/AuthInterceptor.kt (28)
- app/src/main/java/com/example/libraryapp/data/remote/LibraryApiService.kt (22)
- app/src/main/java/com/example/libraryapp/data/remote/MockLibraryInterceptor.kt (198)
- app/src/main/java/com/example/libraryapp/data/remote/NetworkClient.kt (42)
- app/src/main/java/com/example/libraryapp/data/repository/LibraryRepository.kt (114)
- app/src/main/java/com/example/libraryapp/di/ServiceLocator.kt (52)
- app/src/main/java/com/example/libraryapp/ui/navigation/NavGraph.kt (50)
- app/src/main/java/com/example/libraryapp/ui/navigation/Screen.kt (6)
- app/src/main/java/com/example/libraryapp/ui/screens/login/LoginScreen.kt (274)
- app/src/main/java/com/example/libraryapp/ui/screens/login/LoginUiState.kt (12)
- app/src/main/java/com/example/libraryapp/ui/screens/login/LoginViewModel.kt (100)
- app/src/main/java/com/example/libraryapp/ui/screens/profile/ProfileScreen.kt (555)
- app/src/main/java/com/example/libraryapp/ui/screens/profile/ProfileUiState.kt (14)
- app/src/main/java/com/example/libraryapp/ui/screens/profile/ProfileViewModel.kt (73)
- app/src/main/java/com/example/libraryapp/ui/theme/Color.kt (19)
- app/src/main/java/com/example/libraryapp/ui/theme/Theme.kt (46)
- app/src/main/java/com/example/libraryapp/ui/theme/Type.kt (58)
- app/src/main/res/drawable/ic_launcher_background.xml (10)
- app/src/main/res/drawable/ic_launcher_foreground.xml (11)
- app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml (5)
- app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml (5)
- app/src/main/res/values/colors.xml (18)
- app/src/main/res/values/strings.xml (47)
- app/src/main/res/values/themes.xml (7)
- app/src/main/res/xml/backup_rules.xml (5)
- app/src/main/res/xml/data_extraction_rules.xml (9)
- app/src/test/java/com/example/libraryapp/LibraryRepositoryTest.kt (190)
- app/src/test/java/com/example/libraryapp/LoginViewModelTest.kt (141)
- app/src/test/java/com/example/libraryapp/ProfileViewModelTest.kt (65)
- build.gradle.kts (4)
- gradle/wrapper/gradle-wrapper.properties (5)
- gradle.properties (4)
- gradlew (60)
- gradlew.bat (59)
- settings.gradle.kts (17)

## manifest flags
- allowBackup="true"
- android.permission.ACCESS_NETWORK_STATE
- android.permission.INTERNET
- exported="true"

## descriptive counters (aid only)
- todo/fixme/stub: 0
- empty catch: 0
- !! operator: 5
- GlobalScope/runBlocking: 5
- log/println: 0
- hardcoded mock data: 0
- http:// literal: 0
- commented-out code: 0
- @Suppress: 1

## detector findings (rule: n)
- D01: 5
- D02: 35
- P01: 1
- P02: 1
- P08: 1
- P10: 2
- P11: 1
- S02: 1
- S05: 1
- S06: 1
- S08: 2
- U03: 1
- U04: 17
- detekt:LambdaParameterInRestartableEffect: 2
- detekt:ModifierMissing: 7
- lint:DefaultLocale: 1
- lint:MonochromeLauncherIcon: 2
- lint:UnusedResources: 25

## finding sites (first 3 per rule)
- D01 app/build.gradle.kts:36 deprecated kotlinOptions: use compilerOptions DSL (Kotlin Gradle plugin)
- D01 app/build.gradle.kts:81 dependency androidx.security:security-crypto (deprecated)
- D01 app/src/main/java/com/example/libraryapp/data/local/SessionManager.kt:5 deprecated androidx.security-crypto API (all APIs deprecated since 1.1.0)
- D02 /mnt/01DC198BE9192500/github/Schooldev/3S-study-work/build/agy-g38flash-B2-naive-r1/app/build.gradle.kts:13 OldTargetApi: Not targeting the latest versions of Android; compatibility modes apply. Consider testing and up
- D02 /mnt/01DC198BE9192500/github/Schooldev/3S-study-work/build/agy-g38flash-B2-naive-r1/app/build.gradle.kts:53 GradleDependency: A newer version of androidx.core:core-ktx than 1.13.1 is available: 1.19.1
- D02 /mnt/01DC198BE9192500/github/Schooldev/3S-study-work/build/agy-g38flash-B2-naive-r1/app/build.gradle.kts:54 GradleDependency: A newer version of androidx.lifecycle:lifecycle-runtime-ktx than 2.8.0 is available: 2.11.1
- P01 app/src/main/java/com/example/libraryapp/ui/screens/profile/ProfileScreen.kt:229 items(uiState.activeLoans) in LazyColumn has no key
- P02 app/src/main/java/com/example/libraryapp/ui/screens/profile/ProfileScreen.kt:151 LazyColumn with 8 content blocks, no contentType
- P08 app/src/main/java/com/example/libraryapp/ui/screens/profile/ProfileScreen.kt:251 shadow>2dp: defaultElevation = 6.dp) ) { Column( modifier in list item scope DigitalLibraryCard
- P10 app/build.gradle.kts:24 release build type has no isMinifyEnabled = true (R8 off by default)
- P10 build.gradle.kts:1 no release build type: R8 minification off by default
- P11 app/build.gradle.kts:1 no baselineprofile plugin and no baseline-prof.txt
- S02 app/src/main/java/com/example/libraryapp/ui/screens/login/LoginViewModel.kt:89 password = "libraryPass2024", literal secret
- S05 app/src/main/java/com/example/libraryapp/data/remote/NetworkClient.kt:16 HttpLoggingInterceptor Level.BODY not guarded for release
- S06 app/src/main/AndroidManifest.xml:7 allowBackup=true, no backup exclusion rules
- S08 app/build.gradle.kts:81 dependency androidx.security:security-crypto (deprecated since 1.1.0-alpha07/1.1.0)
- S08 app/src/main/java/com/example/libraryapp/data/local/SessionManager.kt:5 deprecated androidx.security-crypto API androidx.security.crypto
- U03 app/src/main/java/com/example/libraryapp/ui/theme/Theme.kt:10 darkColorScheme: onPrimary on primary contrast 4.30:1 < 4.5
- U04 app/src/main/java/com/example/libraryapp/ui/screens/login/LoginScreen.kt:243 hardcoded Color.White, bypasses the theme
- U04 app/src/main/java/com/example/libraryapp/ui/screens/login/LoginScreen.kt:250 hardcoded fontSize = 16.sp, bypasses the theme
- U04 app/src/main/java/com/example/libraryapp/ui/screens/profile/ProfileScreen.kt:117 hardcoded Color.White ) bypasses the theme
- detekt:LambdaParameterInRestartableEffect app/src/main/java/com/example/libraryapp/ui/screens/login/LoginScreen.kt:65 Lambda parameters in a @Composable that are referenced directly inside of restarting effects can cause issues 
- detekt:LambdaParameterInRestartableEffect app/src/main/java/com/example/libraryapp/ui/screens/profile/ProfileScreen.kt:76 Lambda parameters in a @Composable that are referenced directly inside of restarting effects can cause issues 
- detekt:ModifierMissing app/src/main/java/com/example/libraryapp/ui/screens/login/LoginScreen.kt:63 This @Composable function emits content but doesn't have a modifier parameter. See https://mrmans0n.github.io/
- detekt:ModifierMissing app/src/main/java/com/example/libraryapp/ui/screens/profile/ProfileScreen.kt:74 This @Composable function emits content but doesn't have a modifier parameter. See https://mrmans0n.github.io/
- detekt:ModifierMissing app/src/main/java/com/example/libraryapp/ui/screens/profile/ProfileScreen.kt:246 This @Composable function emits content but doesn't have a modifier parameter. See https://mrmans0n.github.io/
- lint:DefaultLocale /mnt/01DC198BE9192500/github/Schooldev/3S-study-work/build/agy-g38flash-B2-naive-r1/app/src/main/java/com/example/libraryapp/ui/screens/profile/ProfileScreen.kt:382 DefaultLocale: Implicitly using the default locale is a common source of bugs: Use `String.format(Locale, ...)
- lint:MonochromeLauncherIcon /mnt/01DC198BE9192500/github/Schooldev/3S-study-work/build/agy-g38flash-B2-naive-r1/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml:2 MonochromeLauncherIcon: The application adaptive icon is missing a monochrome tag
- lint:MonochromeLauncherIcon /mnt/01DC198BE9192500/github/Schooldev/3S-study-work/build/agy-g38flash-B2-naive-r1/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml:2 MonochromeLauncherIcon: The application adaptive roundIcon is missing a monochrome tag
- lint:UnusedResources /mnt/01DC198BE9192500/github/Schooldev/3S-study-work/build/agy-g38flash-B2-naive-r1/app/src/main/res/values/colors.xml:3 UnusedResources: The resource `R.color.purple_80` appears to be unused
- lint:UnusedResources /mnt/01DC198BE9192500/github/Schooldev/3S-study-work/build/agy-g38flash-B2-naive-r1/app/src/main/res/values/colors.xml:4 UnusedResources: The resource `R.color.purple_grey_80` appears to be unused
- lint:UnusedResources /mnt/01DC198BE9192500/github/Schooldev/3S-study-work/build/agy-g38flash-B2-naive-r1/app/src/main/res/values/colors.xml:5 UnusedResources: The resource `R.color.pink_80` appears to be unused

## coder notes (fill in; copy codes to codes.csv)
