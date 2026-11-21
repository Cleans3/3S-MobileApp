# ShelfTap (Android)

Kotlin + Jetpack Compose (Material 3) app for library members to see current loans, renew
them, review reading history, and view book details. Single Gradle module `app`, package
`vn.shelftap.app`, minSdk 26, targetSdk/compileSdk 35.

## Build

1. Generate the Gradle wrapper (the wrapper jar and `gradlew` scripts are not in this tree):
   `gradle wrapper --gradle-version 8.9`, or let Android Studio create them on import.
   `gradle/wrapper/gradle-wrapper.properties` was not written; the wrapper step creates it.
2. Open the project in Android Studio (Ladybug or newer) and sync, or run `./gradlew assembleDebug`.

Nothing here has been compiled or run yet. The environment had no network access, so
dependency resolution and the build have not been checked.

## Layout

- `data/` – models, `LibraryRepository` (network first, encrypted fallback), `SecureCache`
- `net/ApiClient.kt` – OkHttp + kotlinx.serialization client, bearer token per request
- `auth/AuthRepository.kt` – interface only; `UnwiredAuthRepository` is a placeholder
- `ui/loans`, `ui/history`, `ui/book` – screen + ViewModel pairs
- `ui/Navigation.kt` – bottom bar (Loans, History) and the book detail route

## Behaviour

- **Loans**: overdue marker when `dueAt` has passed and `returnedAt` is null; Renew is
  enabled only when `renewable` is true and the request is not in flight. A successful
  renewal replaces the row in place. `RENEWAL_NOT_ALLOWED` (HTTP 409) shows
  "This loan cannot be renewed".
- **History**: newest first; the "Keep my reading history" switch reflects `historyEnabled`.
  Turning it off opens a confirmation dialog; turning it on applies immediately. The
  history list reloads after each change.
- **Book detail**: cover (Coil), title, authors, subjects, copies available.
- **Offline**: Loans and History keep the last successful response, encrypted, and show
  "Showing saved data" when the network is unavailable. Changes to the history setting
  need a connection and report an error otherwise.

## Security and compliance choices

- HTTPS only. `usesCleartextTraffic="false"` plus a network security config with
  `cleartextTrafficPermitted="false"`, trusting system CAs only.
- Personal data (loans, history, member profile) is cached with AES-256-GCM
  (`AES/GCM/NoPadding`). The key lives in the Android Keystore (non-exportable). Each write
  gets a fresh IV from the Cipher, stored as a prefix. Files are in `filesDir/secure-cache`.
  Backup is disabled (`allowBackup="false"`).
- No `androidx.security:security-crypto`. No logging interceptor and no `Log` calls.
- R8 enabled for release (`isMinifyEnabled`, `isShrinkResources`) with keep rules for
  kotlinx.serialization.
- Dependencies are declared in `gradle/libs.versions.toml`. Repositories are `google()` and
  `mavenCentral()` only.
- Every lazy list item has a stable key and content type; images are loaded with `AsyncImage`;
  network I/O runs on `Dispatchers.IO`, injected through `AppContainer`.

## Open items and assumptions

- **Baseline Profile**: the app ships a hand-written seed `app/src/main/baseline-prof.txt` and
  depends on `androidx.profileinstaller`. The `androidx.baselineprofile` plugin and a separate
  `baselineprofile` module are not included, because the project was specified as a single
  module. Generating a real profile needs that module, which should be added.
- **AuthRepository**: the real implementation is assumed to exist elsewhere. Pass it to
  `AppContainer(context, authRepository)` in `ShelfTapApp`. The interface in this project may
  clash with an existing class of the same name; if so, keep only the one from the app.
- **ID types**: `loanId`, `copyId`, `titleId`, `barcode` and `memberId` are assumed to be JSON
  strings. If the API sends numbers, change the models.
- **Error body**: a RENEWAL_NOT_ALLOWED error is read as a top-level `"code"` field in the JSON
  body. If the API nests it, update `errorCodeOf` in `net/ApiClient.kt`.
- **mustChangePassword** is fetched but not shown in the UI; it was not in the spec.
- Day counts use the device time zone. "Overdue" with due time earlier today shows
  "1 day overdue".
- No unit or UI tests were written.
