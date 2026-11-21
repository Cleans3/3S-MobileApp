# ShelfTap member app (Android)

Kotlin, Jetpack Compose with Material 3, minSdk 26, package `vn.shelftap.app`.
Screens: Sign in, Profile, Change password. API: `https://api.shelftap.example`.

## Before the first build

- The Gradle wrapper jar and `gradlew` scripts were not generated (no commands were run).
  Run `gradle wrapper --gradle-version 8.9` once, or open the project in Android Studio.
- `gradle/wrapper/gradle-wrapper.properties` was not written (its write was blocked as a
  sensitive file). Create it with `distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip`
  or let `gradle wrapper` create it.
- Nothing has been compiled or tested. Dependency versions were chosen from known releases
  and should be checked on first sync.

## Design

- **Session storage**: the session (access token, refresh token, `mustChangePassword`) and the
  device ID are serialized to JSON and encrypted with AES-256-GCM. The key lives in the Android
  Keystore and is not exportable. Only the ciphertext is saved, in app-private DataStore.
  `EncryptedSharedPreferences` is not used (CLAUDE.md rule 5).
- **Device ID**: a UUID created on first launch and kept in the same encrypted state.
- **Token refresh**: a call that fails with 401 `TOKEN_INVALID` refreshes once and is repeated.
  Refreshes run under a mutex, so two calls never spend the same single-use refresh token.
  Any refresh failure ends the session and returns the member to Sign in.
- **Sign out**: calls `POST /api/auth/logout`, then deletes the local session even if that call fails.
- **Forced password change**: when `mustChangePassword` is true, the app shows only Change password.
  The same applies after a 403 `PASSWORD_CHANGE_REQUIRED`. Back is disabled there.
- **Error codes**: `INVALID_CREDENTIALS`, `ACCOUNT_LOCKED`, `RATE_LIMITED` (with `Retry-After`),
  `TOKEN_INVALID`, `PASSWORD_CHANGE_REQUIRED`. Messages are in `res/values/strings.xml`.

## Decisions and assumptions

- **Baseline Profile module**: CLAUDE.md rule 19 requires a `baselineprofile` module. It is a
  test-only module (`com.android.test`) that targets `:app`. It is the one addition beyond the
  single `app` module in the brief, and it is required by the project rules.
- The Profile screen shows `memberType` and `status` as the server sends them.
- `memberId`, `accessExpiresIn` and `historyEnabled` are received but not displayed.
- The Profile data is fetched on each visit and not cached on the device (personal data).
- The Baseline Profile covers app start only, because the app has no list screen.
- Logout sends only the refresh token, with no `Authorization` header, as the brief describes.
- Release builds use R8 with `proguard-android-optimize.txt` and keep rules for kotlinx.serialization.
- Only `google()` and `mavenCentral()` repositories are used. No logging interceptor is used.
- Cleartext traffic is off through `res/xml/network_security_config.xml`.
