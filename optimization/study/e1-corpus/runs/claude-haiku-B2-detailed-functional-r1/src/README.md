# ShelfTap member app (Android)

Kotlin, Jetpack Compose with Material 3. Package `vn.shelftap.app`, minSdk 26, single module `app`.

## Opening the project

1. Open this directory in Android Studio. It creates `local.properties` and the Gradle wrapper files.
2. Or run `gradle wrapper --gradle-version 8.9` once in this directory, then `./gradlew assembleDebug`.

The Gradle wrapper jar and `gradlew` scripts are not in this tree. Generate them as in step 2 above.

## Notes

- The project files were written without compiling or running them. Build it before relying on it.
- Tokens and the device id are stored in `EncryptedSharedPreferences`. `deviceId` is created on first launch and kept after sign-out.
- `SessionRepository.authorized` handles `TOKEN_INVALID` with one refresh and retry. A failed refresh returns the member to sign-in.
- A `mustChangePassword` flag, from sign-in or `/api/me`, or a `PASSWORD_CHANGE_REQUIRED` response, shows the forced change-password screen.
- Sign-out calls `/api/auth/logout` when possible and always clears the local session.
- Base URL: `https://api.shelftap.example` (in `data/ApiClient.kt`).
