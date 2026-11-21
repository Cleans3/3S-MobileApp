# ShelfTap (Android)

Kotlin, Jetpack Compose with Material 3, minSdk 26, single app module `app`
(package `vn.shelftap.app`), plus a `baselineprofile` module.

## Screens
- **Loans**: current loans with due date, days left, an "Overdue" marker and a Renew button
  (enabled only when `renewable` is true). `RENEWAL_NOT_ALLOWED` shows "This loan cannot be renewed".
- **History**: past loans, newest first, with a "Keep my reading history" switch. Turning it off
  asks for confirmation first.
- **Book detail**: cover, title, authors, subjects, copies available. Opened from either list.

Loans and History fall back to the last saved data when the network is unavailable and show
"Showing saved data".

## Things to do before the first build
1. **Gradle wrapper.** `gradle/wrapper/gradle-wrapper.properties` was not written. Create it with
   `distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip`
   (Gradle 8.9 is what AGP 8.7.3 requires), or let Android Studio generate the wrapper.
2. **AuthRepository.** The real signed-in `AuthRepository` must be passed to `ShelfTapApi` in
   `AppContainer.kt`. Until then `UnconfiguredAuthRepository` fails every request with HTTP 401,
   so no call reaches the server.
3. **Baseline profile.** Generate it on a device or emulator with `./gradlew :app:generateBaselineProfile`
   after the project syncs.

## Design decisions
- **Offline cache.** Loans, history and member settings are cached as AES-256-GCM ciphertext in
  `filesDir/cache`. The key is in the Android Keystore and is not exportable. No Room, DataStore or
  `security-crypto` is used.
- **Networking.** OkHttp and kotlinx.serialization with HTTPS only. Cleartext is disabled in
  `network_security_config.xml`. Certificate checks are left at the system defaults.
- **Errors.** A network failure (`IOException`) triggers the cache fallback. An HTTP error becomes
  `ApiException`, which carries the API `code`, so `RENEWAL_NOT_ALLOWED` can be recognised.
- **Release build.** R8 minification and resource shrinking are on. Keep rules for kotlinx.serialization
  are in `app/proguard-rules.pro`.
- **Logging.** The app writes no logs, and it does not log tokens or personal data.
- **Dependencies.** Versions are pinned in `gradle/libs.versions.toml`. Repositories are limited to
  `google()` and `mavenCentral()`.

The project has not been compiled in this environment, because no build commands were run.
