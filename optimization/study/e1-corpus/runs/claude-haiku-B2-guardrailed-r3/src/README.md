# ShelfTap member app

Android app (Kotlin, Jetpack Compose, Material 3), minSdk 26, single module `app`, package `vn.shelftap.app`.

## Status

- Project files are written. Nothing has been compiled or run: no network or install commands were run here.
- **Gradle wrapper is incomplete.** `gradle/wrapper/gradle-wrapper.properties` was not written (the write was blocked as a sensitive file). `gradlew`, `gradlew.bat` and `gradle/wrapper/gradle-wrapper.jar` are also missing. Create them once with network access, using Gradle 8.9 (matches `distributionUrl` in the design):
  `gradle wrapper --gradle-version 8.9`
  Or open the folder in Android Studio, which will offer to create them.

## Layout

- `app/src/main/java/vn/shelftap/app/data/`: API client (OkHttp), auth repository, encrypted session store, error model.
- `app/src/main/java/vn/shelftap/app/security/`: AES-256-GCM with an Android Keystore key.
- `app/src/main/java/vn/shelftap/app/ui/`: state, view model, three screens (sign in, profile, change password), theme.

## Decisions

- **Session storage:** the session (device ID, access token, refresh token) is one JSON blob encrypted with AES-256-GCM. The key is in the Android Keystore and is not exportable. The file is `session.enc` in app-private storage. A blob that cannot be decrypted is discarded, and the member signs in again.
- **Device ID:** a random UUID, created on first launch and kept in the encrypted session. It survives sign-out.
- **Refresh:** on `TOKEN_INVALID` the app refreshes once and repeats the call. Refresh runs under a mutex, so concurrent calls share one refresh and the single-use refresh token is never reused. If the refresh fails, or the repeated call also returns `TOKEN_INVALID`, the session is cleared and the sign-in screen shows "session ended".
- **Sign-out:** calls `/api/auth/logout`. The local session is removed even if that call fails.
- **Forced password change:** if sign-in or `/api/me` reports `mustChangePassword` (or the server returns `PASSWORD_CHANGE_REQUIRED`), the app shows only the change-password screen. Because the member needs a way out, that screen offers **Sign out** in this state. This is an addition to the spec.
- **Change password:** reachable from the profile screen (not part of the forced flow). The new password and its confirmation must match. Only blank-field and mismatch checks run on the client; length and strength are left to the server.
- **Error mapping:** API `code` values map to strings in `res/values/strings.xml`. `RATE_LIMITED` shows the `Retry-After` seconds. Unknown codes show a generic message. Server `detail` text is never shown or logged.
- **Logging:** the app has no logging calls and no logging interceptor.
- **Base URL:** `https://api.shelftap.example`, in `BuildConfig`. Cleartext traffic is disabled in the network security config.

## Gaps against the project rules (CLAUDE.md)

- **Rule 19 (Baseline Profile):** the AndroidX baseline-profile generator needs a second module, and the spec asks for a single module. The app ships `app/src/main/baseline-prof.txt`, a hand-written profile covering app start, plus `androidx.profileinstaller`. It does not cover scrolling, because the app has no list. To get a generated profile, add a `baselineprofile` test module later.
- **Rule 13 (lazy lists):** the app has no lists, so no lazy layouts are used.
- **Rule 15 (Coil):** the app has no images, so Coil is not included.
- **Rule 18 (R8):** enabled for release, with keep rules left as a placeholder. Verify that the release build still starts.

## Not yet checked

- Compilation and the release build (R8).
- Behaviour against the real server, including the exact `memberId` type and whether a password change invalidates existing tokens.
