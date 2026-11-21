# ShelfTap (Android)

Catalogue browser for library members. Kotlin, Jetpack Compose (Material 3), minSdk 26, package `vn.shelftap.app`, single app module `app` (plus `baselineprofile`).

## Build

Nothing has been built or run yet. To build:

1. Generate the Gradle wrapper once (the wrapper jar is binary and was not created): `gradle wrapper --gradle-version 8.9`, or open the project in Android Studio, which does this for you.
2. `./gradlew :app:assembleDebug` (or Run in Android Studio).
3. Baseline profile: connect a device (API 28+) and run `./gradlew :app:generateBaselineProfile`.

## How it works

- **Offline-first.** Room is the source of truth. `CatalogueSync` downloads the whole catalogue once (50 titles per page, `GET /api/titles?page=&size=`) through WorkManager, on a network connection. Each page and the progress marker are committed in one transaction, so an interrupted download resumes. Once complete, lists, search and detail work with no network.
- **Title detail** reads the local copy. If the title is not stored, it calls `GET /api/titles/{id}` and stores the result.
- **Search** uses an FTS4 table (`title_search`) over title, authors, ISBN and subjects. Input is debounced (300 ms), and `flatMapLatest` cancels the previous query. Results are paged.
- **Security.** HTTPS only: cleartext is disabled in the network security config, and no covers with non-HTTPS URLs are accepted. The app stores no tokens or personal data (the spec has no sign-in), so no keystore encryption is needed. If sign-in is added, the CLAUDE.md encryption rules apply.
- **Dependencies** come only from `google()` and `mavenCentral()`, all versions are in `gradle/libs.versions.toml`. Room uses `kapt` rather than KSP so that the Gradle plugin resolves from those two repositories.

## Decisions and limitations

- **Search matches word starts, not arbitrary substrings.** FTS4 indexes whole words, so "ell" finds "Ellison" but not "Hello". A literal mid-word substring search would need a full table scan on about 100,000 rows; this was rejected to keep the indexed-search rule. Each typed word is matched as a prefix, and all typed words must match.
- **Empty search box** shows the whole catalogue. Whitespace-only input is treated as empty.
- **No refresh after the first download.** `copiesAvailable` and other fields are as of the download (or the last title fetched by ID). Changes to already-stored titles are written to the title row, but not to the search index.
- The catalogue screen shows an empty-state message until the first page arrives.
- Release builds enable R8 with resource shrinking. Keep rules cover the WorkManager worker and the Room implementation class.

## Tests

`app/src/test/.../FtsQueryTest.kt` covers the search query builder. The baseline profile generator is in `baselineprofile/`. Neither has been run.
