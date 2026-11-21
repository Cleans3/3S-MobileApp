# ShelfTap (Android)

Kotlin, Jetpack Compose + Material 3 app for signed-in library members: current loans
(with renew), reading history (with the "Keep my reading history" switch), and book detail.
Single Gradle module `app`, package `vn.shelftap.app`, minSdk 26, targetSdk/compileSdk 35.

## Assumptions and open items

- **AuthRepository** is not included. It is assumed to exist at
  `vn.shelftap.app.auth.AuthRepository` with `suspend fun accessToken(): String`.
  The sign-in code must assign `AppGraph.authRepository` before the UI loads data.
- **Gradle wrapper** scripts (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`) are
  not included. `gradle/wrapper/gradle-wrapper.properties` was not written, so generate the wrapper
  with a local Gradle 8.9 install (`gradle wrapper --gradle-version 8.9`) or open the project in
  Android Studio, which creates it.
- The project has not been built here (no build was run). Dependency versions are pinned in
  `build.gradle.kts` files: AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01.
- Assumed JSON types: ids (`loanId`, `titleId`, `memberId`) are strings.
- Error bodies are assumed to look like `{"code": "...", "message": "..."}`.
- Offline behaviour: the last successful responses for loans, history and member are kept in
  SharedPreferences (`SavedDataStore`). When the network is unreachable they are shown with
  "Showing saved data".

## Layout

- `data/` – models, REST client (`HttpURLConnection` + kotlinx.serialization), saved-data cache,
  repository.
- `ui/LoansScreen.kt` – loans list, Renew button, Overdue marker, renew errors.
- `ui/HistoryScreen.kt` – past loans newest first, privacy switch with confirmation dialog.
- `ui/TitleScreen.kt` – book detail (cover via Coil, authors, subjects, copies).
- `ui/ShelfTapApp.kt` – tabs (Loans / History) and navigation to book detail.
