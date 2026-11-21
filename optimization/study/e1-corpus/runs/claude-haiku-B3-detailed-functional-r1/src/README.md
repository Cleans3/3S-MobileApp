# ShelfTap (Android)

Kotlin, Jetpack Compose + Material 3, minSdk 26, targetSdk 35, single module `app`, package `vn.shelftap.app`.

## Screens
- **Loans**: current loans with due date, days left, an "Overdue" marker, and a Renew button enabled only when `renewable` is true. A renewed loan shows its new due date. `RENEWAL_NOT_ALLOWED` shows "This loan cannot be renewed".
- **History**: past loans, newest first, with borrowed and returned dates, plus the "Keep my reading history" switch. Turning it off asks for confirmation first.
- **Book detail**: cover, title, authors, subjects, and copies available. Opened from either list.
- Loans and History fall back to the last saved data when the network is unavailable and show "Showing saved data".

## Layout
- `data/` — models, `ShelfTapApi` (OkHttp + kotlinx.serialization), `LibraryRepository` (network with local fallback), `SavedDataStore` (SharedPreferences JSON), `AppContainer`.
- `auth/AuthRepository.kt` — interface with `suspend fun accessToken(): String`.
- `ui/` — navigation, theme, and one folder per screen (`loans`, `history`, `detail`), each with a ViewModel.

## Before building
1. **Gradle wrapper**: `gradle/wrapper/gradle-wrapper.properties` and `gradlew`/`gradlew.bat` are not included. Open the project in Android Studio, or run `gradle wrapper --gradle-version 8.7` once.
2. **AuthRepository**: `auth/AuthRepository.kt` declares the interface. `PlaceholderAuthRepository` throws until the app's real sign-in implementation is passed to `AppContainer`. Replace it there.
3. The project has not been compiled in this environment. Versions are pinned in `build.gradle.kts` and `app/build.gradle.kts` (AGP 8.5.2, Kotlin 2.0.21, Compose BOM 2024.09.00).

## Choices made
- Saved data is kept per resource (loans, history, member profile). The last successful response is saved, and a fresh response replaces it. A renew also updates the saved loan list.
- Only network errors (`IOException`) trigger the saved-data fallback. Server errors are shown as errors.
- Turning history on applies immediately. Turning it off requires confirmation.
- Renewing a loan while offline is not queued. It shows an error message.
