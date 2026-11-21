# ShelfTap member app (Android)

Kotlin, Jetpack Compose with Material 3, minSdk 26, package `vn.shelftap.app`.

## Opening the project

1. Generate the Gradle wrapper once (the wrapper jar and `gradlew` scripts are not included):
   `gradle wrapper --gradle-version 8.9`
   Or open the folder in Android Studio, which creates them on sync.
2. Build with `./gradlew :app:assembleDebug`.
3. Generate the baseline profile with `./gradlew :app:generateBaselineProfile` on a device or emulator.

## Structure

- `app`: the single app module.
- `baselineprofile`: a test-only module (`com.android.test`) that the project's Baseline Profile rule requires. It holds no app code.
- `gradle/libs.versions.toml`: every dependency and plugin version.

## Behaviour

- **Session**: tokens are encrypted with AES-256-GCM. The key is non-exportable in the Android Keystore, and the ciphertext is stored in app-private DataStore. A member stays signed in across restarts.
- **Token refresh**: on `401 TOKEN_INVALID` the app refreshes once and repeats the call. Refreshes are serialised because refresh tokens are single use. If the refresh fails, the session is cleared and the member goes to sign-in.
- **Sign out**: calls `POST /api/auth/logout`, then removes the session locally even if the call fails.
- **Required password change**: if `mustChangePassword` is true (at sign-in, on `/api/me`, or on `403 PASSWORD_CHANGE_REQUIRED`), the member is held on the change-password screen. Back navigation is disabled there.
- **Device id**: a random UUID, created once and kept in DataStore.

## Decisions taken without a spec

- The app has no list screens, so the baseline profile covers app start only. The rule's "scroll the main list" part does not apply.
- Form validation only checks for empty fields and matching new passwords. The server sets password policy.
- A network failure during token refresh keeps the session. Only a server rejection signs the member out.
- Error text is chosen by API error code. Server `detail` text is never shown or logged.

## Not verified

This project was written without a build, so it has not been compiled or run. Build it first and fix any compile errors.
