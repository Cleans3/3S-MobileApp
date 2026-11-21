# ShelfTap catalogue (Android)

Kotlin, Jetpack Compose with Material 3, minSdk 26, single module `app`, package `vn.shelftap.app`.

## Opening and building

- Open the folder in Android Studio, or run `gradle wrapper --gradle-version 8.9` once to create the Gradle wrapper (the wrapper jar and `gradlew` scripts are not included).
- `local.properties` (the Android SDK path) is not included; Android Studio creates it.

## Design decisions

- **Offline catalogue.** On first launch the app downloads the whole catalogue with `GET /api/titles?page=&size=50` and stores it in Room. Downloading is resumable: the next page and the total are saved after each page, so a retry continues where it stopped. Browsing and searching read only from Room, so they work with no connection once the download has finished.
- **Search.** Matches title, author, ISBN and subject as a case-insensitive substring. Each row stores a lowercased `searchText` blob, and user input is escaped for LIKE. Results update as you type, with a 200 ms debounce.
- **Paging.** Lists use Paging 3 over Room, so 100,000 titles are never loaded into memory at once.
- **Title detail.** Shows the locally stored title and tries to refresh it from `GET /api/titles/{id}`. If the refresh fails, the cached copy stays on screen.
- **Covers.** Coil loads covers and caches them on disk when seen. Covers are not downloaded during the catalogue sync (about 100k images), so a cover that was never viewed online shows a "No cover" placeholder offline.
- **Catalogue refresh.** There is no in-app "update catalogue" action. To refresh, clear the app data or add a reset action.
- **Dependencies.** Versions are pinned in `build.gradle.kts` files. The project has not been compiled in this environment.
