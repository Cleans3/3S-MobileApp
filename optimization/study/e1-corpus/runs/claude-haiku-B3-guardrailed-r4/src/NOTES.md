# ShelfTap Android project: notes

## Before the first build
- `gradle/wrapper/gradle-wrapper.properties` was not written (the write was refused as a sensitive file). Create it with `distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip`, or let Android Studio / `gradle wrapper` generate the wrapper files (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`).
- Nothing has been compiled. Sync and build in Android Studio (JDK 17+).
- Repositories are `google()` and `mavenCentral()` only, as CLAUDE.md requires. If plugin resolution fails for a Kotlin or AndroidX plugin, that is the first place to look.

## Auth
- `auth/AuthRepository.kt` declares the interface the spec assumes (`suspend fun accessToken(): String`).
- `UnavailableAuthRepository` is a placeholder that always throws. Replace it in `ShelfTapApp.container` with the app's real repository. Until then, every API call fails with a generic error.

## API assumptions (adjust if the server differs)
- Ids may be JSON strings or numbers; both are read as strings.
- `authors` and `subjects` are arrays of strings. `coverUrl`, `returnedAt` may be null.
- The renewal error body carries a top-level `"code"` field (`RENEWAL_NOT_ALLOWED`) with HTTP 409.
- `/api/me/history` is called together with `/api/me` so the history switch has its value.

## Offline behaviour
- Loans and history are stored as AES-256-GCM ciphertext in `filesDir/secure-cache`, with the key in the Android Keystore (`data/EncryptedCache.kt`). Each write refreshes the copy after a successful network read.
- When a read fails with a network error, the saved copy is shown with the "Showing saved data" note. If there is no saved copy, a message with a retry button is shown.
- Turning history off replaces the saved history with an empty one.

## Build settings
- minSdk 26, compileSdk/targetSdk 35, AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01.
- Release builds use R8 (`isMinifyEnabled`, `isShrinkResources`).
- Baseline Profile: the `baselineprofile` module (test module) records app start and a fling on the loans list. Run `generateBaselineProfile` on a device or emulator.
