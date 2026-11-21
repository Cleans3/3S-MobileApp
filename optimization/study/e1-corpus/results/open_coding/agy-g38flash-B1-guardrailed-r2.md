# agy-g38flash-B1-guardrailed-r2
bundle=agy-g38flash style=guardrailed brief=B1 rep=2 files=47 kotlin_loc=1457 build=pass

## files
- app/build.gradle.kts (102)
- app/proguard-rules.pro (30)
- app/src/main/AndroidManifest.xml (30)
- app/src/main/baseline-prof.txt (8)
- app/src/main/java/vn/shelftap/app/MainActivity.kt (27)
- app/src/main/java/vn/shelftap/app/ShelfTapApplication.kt (28)
- app/src/main/java/vn/shelftap/app/data/api/NetworkModule.kt (38)
- app/src/main/java/vn/shelftap/app/data/api/ShelfTapApi.kt (19)
- app/src/main/java/vn/shelftap/app/data/api/TitleDto.kt (36)
- app/src/main/java/vn/shelftap/app/data/local/AppDatabase.kt (39)
- app/src/main/java/vn/shelftap/app/data/local/Converters.kt (24)
- app/src/main/java/vn/shelftap/app/data/local/TitleDao.kt (65)
- app/src/main/java/vn/shelftap/app/data/local/TitleEntity.kt (54)
- app/src/main/java/vn/shelftap/app/data/local/TitleFtsEntity.kt (14)
- app/src/main/java/vn/shelftap/app/data/mediator/CatalogueRemoteMediator.kt (82)
- app/src/main/java/vn/shelftap/app/data/mediator/RemoteKeyEntity.kt (28)
- app/src/main/java/vn/shelftap/app/data/repository/CatalogueRepository.kt (12)
- app/src/main/java/vn/shelftap/app/data/repository/CatalogueRepositoryImpl.kt (95)
- app/src/main/java/vn/shelftap/app/data/security/KeystoreEncryptionManager.kt (57)
- app/src/main/java/vn/shelftap/app/domain/model/Title.kt (12)
- app/src/main/java/vn/shelftap/app/ui/catalogue/AvailabilityBadge.kt (47)
- app/src/main/java/vn/shelftap/app/ui/catalogue/CatalogueScreen.kt (219)
- app/src/main/java/vn/shelftap/app/ui/catalogue/CatalogueViewModel.kt (53)
- app/src/main/java/vn/shelftap/app/ui/catalogue/TitleItemRow.kt (127)
- app/src/main/java/vn/shelftap/app/ui/detail/TitleDetailScreen.kt (264)
- app/src/main/java/vn/shelftap/app/ui/detail/TitleDetailViewModel.kt (67)
- app/src/main/java/vn/shelftap/app/ui/navigation/ShelfTapNavHost.kt (71)
- app/src/main/java/vn/shelftap/app/ui/theme/Color.kt (17)
- app/src/main/java/vn/shelftap/app/ui/theme/Theme.kt (48)
- app/src/main/java/vn/shelftap/app/ui/theme/Type.kt (46)
- app/src/main/java/vn/shelftap/app/util/DispatcherProvider.kt (16)
- app/src/main/res/drawable/ic_launcher_foreground.xml (12)
- app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml (5)
- app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml (5)
- app/src/main/res/values/colors.xml (12)
- app/src/main/res/values/strings.xml (22)
- app/src/main/res/values/themes.xml (7)
- app/src/main/res/xml/network_security_config.xml (5)
- baselineprofile/build.gradle.kts (34)
- baselineprofile/src/main/AndroidManifest.xml (2)
- baselineprofile/src/main/java/vn/shelftap/baselineprofile/BaselineProfileGenerator.kt (34)
- build.gradle.kts (9)
- gradle/libs.versions.toml (66)
- gradle/wrapper/gradle-wrapper.properties (7)
- gradlew (7)
- gradlew.bat (5)
- settings.gradle.kts (17)

## manifest flags
- allowBackup="false"
- android.permission.ACCESS_NETWORK_STATE
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
- @Suppress: 2

## detector findings (rule: n)
- D01: 2
- D02: 111
- P10: 1
- S06: 1
- S11: 1
- U05: 1
- lint:MonochromeLauncherIcon: 2
- lint:ObsoleteLintCustomCheck: 3
- lint:ObsoleteSdkInt: 1
- lint:PluralsCandidate: 1
- lint:RedundantLabel: 1
- lint:UnusedResources: 11

## finding sites (first 3 per rule)
- D01 app/build.gradle.kts:50 deprecated kotlinOptions: use compilerOptions DSL (Kotlin Gradle plugin)
- D01 baselineprofile/build.gradle.kts:24 deprecated kotlinOptions: use compilerOptions DSL (Kotlin Gradle plugin)
- D02 /mnt/01DC198BE9192500/github/Schooldev/3S-study-work/build/agy-g38flash-B1-guardrailed-r2/app/build.gradle.kts:17 OldTargetApi: Not targeting the latest versions of Android; compatibility modes apply. Consider testing and up
- D02 /mnt/01DC198BE9192500/github/Schooldev/3S-study-work/build/agy-g38flash-B1-guardrailed-r2/gradle/libs.versions.toml:2 AndroidGradlePluginVersion: A newer version of com.android.application than 8.7.2 is available: 9.5.2. (There 
- D02 /mnt/01DC198BE9192500/github/Schooldev/3S-study-work/build/agy-g38flash-B1-guardrailed-r2/gradle/libs.versions.toml:2 AndroidGradlePluginVersion: A newer version of com.android.application than 8.7.2 is available: 9.5.2. (There 
- P10 build.gradle.kts:1 no release build type: R8 minification off by default
- S06 /mnt/01DC198BE9192500/github/Schooldev/3S-study-work/build/agy-g38flash-B1-guardrailed-r2/app/src/main/AndroidManifest.xml:9 DataExtractionRules: The attribute `android:allowBackup` is deprecated from Android 12 and higher and may be r
- S11 gradle/libs.versions.toml:65 hallucinated-version com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.0.21-1.0.28 (repos: googl
- U05 app/src/main/java/vn/shelftap/app/ui/detail/TitleDetailScreen.kt:1 data screen file renders no error state
- lint:MonochromeLauncherIcon /mnt/01DC198BE9192500/github/Schooldev/3S-study-work/build/agy-g38flash-B1-guardrailed-r2/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml:2 MonochromeLauncherIcon: The application adaptive icon is missing a monochrome tag
- lint:MonochromeLauncherIcon /mnt/01DC198BE9192500/github/Schooldev/3S-study-work/build/agy-g38flash-B1-guardrailed-r2/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml:2 MonochromeLauncherIcon: The application adaptive roundIcon is missing a monochrome tag
- lint:ObsoleteLintCustomCheck /mnt/01DC198BE9192500/github/Schooldev/3S-study-work/build/agy-g38flash-B1-guardrailed-r2/app/build/intermediates/lint-cache/lintReportDebug/migrated-jars/androidx.navigation.common.lint.NavigationCommonIssueRegistry-5daa12651be0734e..jar: ObsoleteLintCustomCheck: Library lint checks out of date;
these checks **will be skipped**!

Lint found an iss
- lint:ObsoleteLintCustomCheck /mnt/01DC198BE9192500/github/Schooldev/3S-study-work/build/agy-g38flash-B1-guardrailed-r2/app/build/intermediates/lint-cache/lintReportDebug/migrated-jars/androidx.navigation.compose.lint.NavigationComposeIssueRegistry-598b224f0ce2f9f2..jar: ObsoleteLintCustomCheck: Library lint checks out of date;
these checks **will be skipped**!

Lint found an iss
- lint:ObsoleteLintCustomCheck /mnt/01DC198BE9192500/github/Schooldev/3S-study-work/build/agy-g38flash-B1-guardrailed-r2/app/build/intermediates/lint-cache/lintReportDebug/migrated-jars/androidx.navigation.runtime.lint.NavigationRuntimeIssueRegistry-a8fea42aedb9bdb3..jar: ObsoleteLintCustomCheck: Library lint checks out of date;
these checks **will be skipped**!

Lint found an iss
- lint:ObsoleteSdkInt /mnt/01DC198BE9192500/github/Schooldev/3S-study-work/build/agy-g38flash-B1-guardrailed-r2/app/src/main/res/mipmap-anydpi-v26: ObsoleteSdkInt: This folder configuration (`v26`) is unnecessary; `minSdkVersion` is 26. Merge all the resourc
- lint:PluralsCandidate /mnt/01DC198BE9192500/github/Schooldev/3S-study-work/build/agy-g38flash-B1-guardrailed-r2/app/src/main/res/values/strings.xml:17 PluralsCandidate: Formatting %d followed by words ("copies"): This should probably be a plural rather than a s
- lint:RedundantLabel /mnt/01DC198BE9192500/github/Schooldev/3S-study-work/build/agy-g38flash-B1-guardrailed-r2/app/src/main/AndroidManifest.xml:20 RedundantLabel: Redundant label can be removed
- lint:UnusedResources /mnt/01DC198BE9192500/github/Schooldev/3S-study-work/build/agy-g38flash-B1-guardrailed-r2/app/src/main/res/values/colors.xml:3 UnusedResources: The resource `R.color.primary` appears to be unused
- lint:UnusedResources /mnt/01DC198BE9192500/github/Schooldev/3S-study-work/build/agy-g38flash-B1-guardrailed-r2/app/src/main/res/values/colors.xml:4 UnusedResources: The resource `R.color.on_primary` appears to be unused
- lint:UnusedResources /mnt/01DC198BE9192500/github/Schooldev/3S-study-work/build/agy-g38flash-B1-guardrailed-r2/app/src/main/res/values/colors.xml:6 UnusedResources: The resource `R.color.on_primary_container` appears to be unused

## coder notes (fill in; copy codes to codes.csv)
