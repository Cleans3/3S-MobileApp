# Library app (Android)

Jetpack Compose app for library members: sign in with email and password, stay signed in across launches, and view your profile.

## Build

Open the project in Android Studio (Ladybug or newer) and let it sync, or run `gradle wrapper --gradle-version 8.10.2` once to generate the wrapper scripts and jar, then `./gradlew assembleDebug`.

The API base URL is set in `app/build.gradle.kts` as `LIBRARY_API_BASE_URL` (default `https://library.example.com/`). Change it to your server.

## REST API contract

| Method | Path | Request | Response |
|---|---|---|---|
| POST | `api/v1/auth/login` | `{"email": "...", "password": "..."}` | `{"token": "..."}` |
| GET | `api/v1/members/me` | `Authorization: Bearer <token>` | `{"id", "fullName", "email", "memberNumber", "membershipStatus", "joinedOn"}` |
| POST | `api/v1/auth/logout` | `Authorization: Bearer <token>` | 2xx (response ignored) |

- A 401 from login shows "Email or password is incorrect."
- A 401 from `members/me` ends the local session and returns to sign-in.

## Design notes

- The session token is stored in Jetpack DataStore (`TokenStore`). Backups are disabled (`allowBackup=false`) so the token is not exported.
- `AuthInterceptor` attaches the token to every request.
- `AuthViewModel` drives the screens from a single `AuthUiState`. `LibraryRoot` switches between loading, sign-in, and profile.
