# Library Android app: member sign-in and profile

Kotlin + Jetpack Compose app. A member signs in with email and password, stays
signed in across launches, and can view their profile. Talks to the library REST API.

## Build setup

- Gradle 8.9 (wrapper properties to be generated, see below), AGP 8.7.3, Kotlin 2.0.21
- compileSdk / targetSdk 35, minSdk 26, Java 17
- Retrofit 2.11 + Gson, OkHttp 4.12, Compose BOM 2024.12.01
- `androidx.security:security-crypto` for the encrypted session token

**Not included:** the Gradle wrapper jar and `gradlew` scripts, and
`gradle/wrapper/gradle-wrapper.properties` (its write was blocked as a sensitive file).
To finish the wrapper, run once with network access from a machine that has Gradle:

    gradle wrapper --gradle-version 8.9

## Configuration

- API base URL: `LIBRARY_API_BASE_URL` in `app/build.gradle.kts` (placeholder
  `https://library.example.com/`). Must end with `/`.

## REST API contract (assumed; adjust to the real service)

| Method | Path                  | Request                     | Response                                        |
|--------|-----------------------|-----------------------------|-------------------------------------------------|
| POST   | `api/v1/auth/login`   | `{"email","password"}`      | `{"token": "..."}`                              |
| GET    | `api/v1/members/me`   | header `Authorization: Bearer <token>` | `{"id","email","fullName","membershipStatus"?,"memberSince"?}` |

## Behaviour

- Token is stored in `EncryptedSharedPreferences` (`data/SessionStore.kt`) and attached
  to every request by `data/AuthInterceptor.kt`.
- Sign-in logs in, saves the token, then loads the profile. If the profile load fails,
  the token is discarded.
- On app start with a saved token, the profile is loaded. A network error keeps the
  member signed in and shows a message. A 401 clears the session and returns to sign-in.
- Sign-out clears the stored token.

## Layout

- `LibraryApplication.kt`: builds the OkHttp/Retrofit stack and the repository
- `MainActivity.kt`: shows `ProfileScreen` or `LoginScreen` depending on session state
- `data/`: models, Retrofit interface, session store, auth interceptor, repository
- `ui/`: `AccountViewModel`, `LoginScreen`, `ProfileScreen`, theme

## Not done

- No automated tests. Error messages are hard-coded English strings in Kotlin.
- Nothing was compiled or run: this environment had no network access.
