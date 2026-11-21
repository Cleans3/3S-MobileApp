# ShelfTap (Android)

Kotlin, Jetpack Compose with Material 3, minSdk 26, single app module `app`, package `vn.shelftap.app`.
A `baselineprofile` module generates the Baseline Profile.

## Screens
- **Loans**: one row per loan with title, due date, days left, an "Overdue" marker, and a Renew button enabled only when `renewable` is true. A renewed row shows the new due date. `RENEWAL_NOT_ALLOWED` (HTTP 409) shows "This loan cannot be renewed".
- **History**: past loans, newest first, with borrowed and returned dates, and the "Keep my reading history" switch. Turning it off asks for confirmation first.
- **Book detail**: cover, title, authors, subjects, and copies available. Opened from a title in either list.
- Loans and History show the last loaded data with "Showing saved data" when the network is unavailable (or the server returns 5xx).

## Structure
- `data/ShelfTapApi.kt`: REST client (OkHttp, org.json). Every call runs on the injected dispatcher and sends `Authorization: Bearer <token>`.
- `data/LibraryRepository.kt`: remote-first loading with fallback to the saved copy.
- `data/SavedDataStore.kt` + `data/KeystoreCipher.kt`: the saved copy is AES-256-GCM encrypted with a non-exportable Android Keystore key, and only ciphertext is written to `filesDir/saved_data.bin`.
- `ui/…`: one ViewModel and one screen per feature. Dates and day counts are formatted in ViewModels, not composables.

## Decisions I made (not specified)
- Error bodies are read for a top-level `"code"` field. A 409 on the renew endpoint is treated as `RENEWAL_NOT_ALLOWED` even if the body is unparseable.
- A 5xx or network failure falls back to saved data. Other 4xx errors show an error message and do not fall back.
- Turning history on saves immediately. Turning it off asks first. The switch changes only after the server confirms.
- Loans, history, and the history-enabled flag are the only data saved on device. Book details are not cached.
- Saved data is not cleared on sign-out, because no sign-out API was specified. Add a `SavedDataStore` wipe call to the sign-out path when it exists.
- Backups are disabled (`allowBackup="false"`) because the Keystore key does not move between devices and an encrypted file without its key is useless.
- Cleartext traffic is disabled in the network security config. No `http://` base URL is used.

## Things that need your action
1. **AuthRepository is a placeholder** at `app/src/main/java/vn/shelftap/app/auth/AuthRepository.kt`. The app already has one, so replace this file with the real class. Only `suspend fun accessToken(): String` is used.
2. **`gradle/wrapper/gradle-wrapper.properties` was not written.** Writing that file was blocked by a permission check. Create it with a local Gradle install (`gradle wrapper --gradle-version 8.9`), or copy it from another Android project. The wrapper JAR and scripts are also not included.
3. **The project has not been built.** I had no network access and did not run Gradle. Versions are pinned in `gradle/libs.versions.toml`: AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01, Gradle 8.9. If a version does not resolve, change it there.
4. **Baseline profile**: run `./gradlew :app:generateBaselineProfile` on a device or emulator with the member signed in. Without signed-in data, the list-scroll part of the generator does little.
5. The launcher icon is a simple placeholder vector.
