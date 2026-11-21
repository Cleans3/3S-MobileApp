# ShelfTap member app (Android)

Kotlin, Jetpack Compose with Material 3, minSdk 26, single Gradle module `app`, package `vn.shelftap.app`.

## Before the first build

- **Gradle wrapper:** `gradle/wrapper/gradle-wrapper.properties` and the `gradlew` scripts are not in this tree. Run `gradle wrapper --gradle-version 8.10.2` once, or open the project in Android Studio, which creates them.
- **Not compiled or tested here.** The project was written without building it. The dependency versions in `gradle/libs.versions.toml` are unverified against the repositories.
- **Plugin resolution:** only `google()` and `mavenCentral()` are configured, as the project rules require. If the Kotlin plugin markers do not resolve from Maven Central, that is the first thing to check.

## Structure

- `data/SessionStore.kt`: device ID and tokens in one AES-256-GCM file. The key is in the Android Keystore.
- `data/ApiClient.kt`: HTTPS-only OkHttp client. It maps API errors to `ApiException`.
- `data/AccountRepository.kt`: sign in, sign out, `/api/me`, change password, and the 401 `TOKEN_INVALID` refresh-and-retry.
- `ui/AccountViewModel.kt`: screen state. Sign-in, profile, and forced password change.
- `ui/*Screen.kt`: sign-in, profile, and change-password screens.

## Decisions

- **Baseline profile:** the rules ask for a separate `baselineprofile` module. The brief asks for a single module, so I kept one module. `app/src/main/baseline-prof.txt` is a hand-written profile for app start, and `profileinstaller` is included. Generate a real one with Macrobenchmark later.
- **Refresh failure:** any failed refresh clears the stored session and returns the member to sign-in. This includes a network error during refresh, because the old refresh token is single use.
- **Sign-out:** the local session is removed even when the logout call fails.
- **Lost Keystore key or damaged session file:** treated as signed out. Backups are disabled (`allowBackup="false"`) so ciphertext is never restored without its key.
- **Back button on the change-password screen:** does nothing while the change is forced.
- **Member data:** profile data is held in memory only. Nothing is logged.
